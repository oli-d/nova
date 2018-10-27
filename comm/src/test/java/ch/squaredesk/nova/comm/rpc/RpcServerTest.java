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

import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;
import ch.squaredesk.nova.metrics.Metrics;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpcServerTest {

    @Test
    void canOnlyCreateInstanceWithMetricsObject() {
        Throwable t = assertThrows(NullPointerException.class, () -> new MyRpcServer(null));
        assertThat(t.getMessage(), containsString("metrics"));
    }

    private class MyRpcServer extends RpcServer<String, RpcInvocation<String, IncomingMessageMetaData<Void, Void>, String, Void>> {

        MyRpcServer(Metrics metrics) {
            super(metrics);
        }

        @Override
        public <T> Flowable<? extends RpcInvocation<T, ? extends IncomingMessageMetaData<?, ?>, RpcInvocation<String, IncomingMessageMetaData<Void, Void>, String, Void>, ?>> requests(String destination, MessageUnmarshaller<RpcInvocation<String, IncomingMessageMetaData<Void, Void>, String, Void>, T> requestUnmarshaller) {
            return Flowable.empty();
        }
    }

}