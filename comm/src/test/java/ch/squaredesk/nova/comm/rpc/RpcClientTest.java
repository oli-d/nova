/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.comm.sending.MessageSendingInfo;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Single;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpcClientTest {
    @Test
    void canOnlyCreateInstanceWithMetricsObject() {
        Throwable t = assertThrows(NullPointerException.class, () -> new MyRpcClient(null));
        assertThat(t.getMessage(), containsString("metrics"));
    }

    private class MyRpcClient extends RpcClient<String,String,Void> {
        protected MyRpcClient(Metrics metrics) {
            super(metrics);
        }

        @Override
        public <RequestType extends String, ReplyType extends String> Single<ReplyType> sendRequest(RequestType request, MessageSendingInfo<String, Void> messageSendingInfo, long timeout, TimeUnit timeUnit) {
            return Single.just((ReplyType) request);
        }
    }

}