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

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.sending.OutgoingMessageMetaData;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpcClientTest {
    @Test
    void canOnlyCreateInstanceWithMetricsObject() {
        Throwable t = assertThrows(NullPointerException.class, () -> new MyRpcClient(null));
        assertThat(t.getMessage(), containsString("metrics"));
    }

    private class MyRpcClient extends RpcClient<
            String,
            OutgoingMessageMetaData<String, Void>,
            IncomingMessageMetaData<String, Void>> {
        MyRpcClient(Metrics metrics) {
            super(metrics);
        }

        @Override
        public <RequestType, ReplyType> Single<? extends RpcReply<ReplyType, IncomingMessageMetaData<String, Void>>> sendRequest(RequestType request, OutgoingMessageMetaData<String, Void> requestMetaData, Function<RequestType, String> requestTranscriber, Function<String, ReplyType> replyTranscriber, long timeout, TimeUnit timeUnit) {
            return null;
        }
    }

}