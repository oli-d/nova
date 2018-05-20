/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import java.net.URL;

import static java.util.concurrent.TimeUnit.SECONDS;

class RequestSender {
    private final String baseUrl;
    private final RpcClient<String> rpcClient;

    private RequestSender(String baseUrl, RpcClient<String> rpcClient) {
        this.baseUrl = baseUrl;
        this.rpcClient = rpcClient;
    }

    static RequestSender createFor (String baseUrl, RpcClient<String> rpcClient) {
        return new RequestSender(baseUrl, rpcClient);
    }

    void sendPostRestRequestInNewThread(String path) {
        sendPostRestRequestInNewThread(path, "{}");
    }

    void sendPostRestRequestInNewThread(String path, String payload) {
        if (path != null && !path.trim().startsWith("/")) {
            path = "/" + path.trim();
        }
        String pathToUseForLambda = path;
        new Thread(() -> {
            try {
                RequestMessageMetaData meta = new RequestMessageMetaData(
                    new URL(baseUrl + pathToUseForLambda),
                    new RequestInfo(HttpRequestMethod.POST));
                rpcClient.sendRequest(payload, meta, 15, SECONDS).blockingGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
