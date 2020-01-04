/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.DefaultMessageTranscriberForStringAsTransportType;
import ch.squaredesk.nova.comm.MessageTranscriber;

import java.net.URL;

import static java.util.concurrent.TimeUnit.SECONDS;

class RequestSender {
    private final String baseUrl;
    private final RpcClient rpcClient;
    private final MessageTranscriber<String> messageTranscriber = new DefaultMessageTranscriberForStringAsTransportType();

    private RequestSender(String baseUrl, RpcClient rpcClient) {
        this.baseUrl = baseUrl;
        this.rpcClient = rpcClient;
    }

    static RequestSender createFor (String baseUrl, RpcClient rpcClient) {
        return new RequestSender(baseUrl, rpcClient);
    }

    void sendPostRestRequestInNewThread(String path) {
        sendPostRestRequestInNewThread(path, "{}");
    }

    void sendPostRestRequestInNewThread(String path, String payload) {
        String pathToUse;
        if (path != null && !path.trim().startsWith("/")) {
            pathToUse = "/" + path.trim();
        } else {
            pathToUse = path;
        }
        new Thread(() -> {
            try {
                RequestMessageMetaData meta = new RequestMessageMetaData(
                    new URL(baseUrl + pathToUse),
                    new RequestInfo(HttpRequestMethod.POST));
                rpcClient.sendRequest(payload, meta, messageTranscriber.getOutgoingMessageTranscriber(String.class), messageTranscriber.getIncomingMessageTranscriber(String.class), 15, SECONDS).blockingGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
