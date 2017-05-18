/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.CharBuffer;

class UrlInvoker<InternalMessageType>  {
    private final Logger logger = LoggerFactory.getLogger(UrlInvoker.class);

    UrlInvoker(String identifier,
               Metrics metrics) {
    }

    Single<String> fireRequest(String request, MessageSendingInfo<URL, HttpSpecificInfo> sendingInfo) {
        // TODO reuse HttpClient
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(3000)
                .setConnectTimeout(3000)
                .build();

        CloseableHttpAsyncClient httpClient = HttpAsyncClients
                .custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        return Single.<String>create(s -> {
            httpClient.start();
            FutureCallback<HttpCallResult> myCallback = new FutureCallback<HttpCallResult>() {
                @Override
                public void completed(HttpCallResult result) {
                    if (result.status.getStatusCode()<200 || result.status.getStatusCode()>299) {
                        s.onError(new RuntimeException("Server answered with error " + result.status + " / " + result.response));
                    } else {
                        s.onSuccess(result.response);
                    }
                }

                @Override
                public void failed(Exception ex) {
                    s.onError(ex);
                }

                @Override
                public void cancelled() {
                    s.onError(new RuntimeException("Cancelled"));
                }
            };

            if (sendingInfo.transportSpecificInfo != null && sendingInfo.transportSpecificInfo.requestMethod == HttpRequestMethod.GET) {
                httpClient.execute(
                        HttpAsyncMethods.createGet(sendingInfo.destination.toURI()),
                        new MyResponseConsumer(),
                        myCallback);
            } else if (sendingInfo.transportSpecificInfo != null && sendingInfo.transportSpecificInfo.requestMethod == HttpRequestMethod.PUT) {
                httpClient.execute(
                        HttpAsyncMethods.createPut(sendingInfo.destination.toURI(), request, ContentType.APPLICATION_JSON),
                        new MyResponseConsumer(),
                        myCallback);
            } else {
                // default method: POST
                httpClient.execute(
                        HttpAsyncMethods.createPost(sendingInfo.destination.toURI(), request, ContentType.APPLICATION_JSON),
                        new MyResponseConsumer(),
                        myCallback);
            }
        }).doFinally(() -> {
            try {
                httpClient.close();
            } catch (Throwable t) {
                logger.error("An error occurred, trying to close the HTTP connection to " + sendingInfo.destination, t);
            }
        });
    }

    private class MyResponseConsumer extends AsyncCharConsumer<HttpCallResult> {
        private final StringBuilder sb = new StringBuilder();
        private HttpResponse response;

        @Override
        protected void onResponseReceived(final HttpResponse response) {
            this.response = response;
        }

        @Override
        protected void onCharReceived(final CharBuffer buf, final IOControl ioctrl) throws IOException {
            while (buf.hasRemaining()) {
                sb.append(buf.get());
            }
        }

        @Override
        protected void releaseResources() {
        }

        @Override
        protected HttpCallResult buildResult(final HttpContext context) {
            return new HttpCallResult(response.getStatusLine(),sb.toString());
        }

    }
}
