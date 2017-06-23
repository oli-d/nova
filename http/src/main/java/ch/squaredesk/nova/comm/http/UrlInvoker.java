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
import okhttp3.*;
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

class UrlInvoker  {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Logger logger = LoggerFactory.getLogger(UrlInvoker.class);

    private final OkHttpClient client = new OkHttpClient();

    UrlInvoker(String identifier,
               Metrics metrics) {
    }

    Single<String> fireRequest(String request, MessageSendingInfo<URL, HttpSpecificInfo> sendingInfo) {
        // TODO capture request metrics
        Request.Builder requestBuilder = new Request.Builder()
                .url(sendingInfo.destination)
                ;
        Request httpRequest;
        if (sendingInfo.transportSpecificInfo.requestMethod==HttpRequestMethod.POST) {
            RequestBody body = RequestBody.create(JSON, request);
            httpRequest = requestBuilder.post(body).build();
        } else if (sendingInfo.transportSpecificInfo.requestMethod==HttpRequestMethod.PUT) {
            RequestBody body = RequestBody.create(JSON, request);
            httpRequest = requestBuilder.put(body).build();
        } else {
            httpRequest = requestBuilder.get().build();
        }

        return Single.fromCallable(() -> {
            Response response = client.newCall(httpRequest).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new RuntimeException(response.message());
            }
        });
    }
}
