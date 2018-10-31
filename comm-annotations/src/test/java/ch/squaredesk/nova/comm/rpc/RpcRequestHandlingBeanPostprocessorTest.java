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

package ch.squaredesk.nova.comm.rpc;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.retrieving.IncomingMessage;
import ch.squaredesk.nova.comm.retrieving.IncomingMessageMetaData;
import io.reactivex.functions.Function;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.hamcrest.junit.MatcherAssert.assertThat;

class RpcRequestHandlingBeanPostprocessorTest {
    private ApplicationContext appContext;

    @BeforeEach
    void setUp() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        appContext = ctx;
    }

    @Test
    void rpcRequestProcessorAutomaticallyWiredUpDuringSpringContextInitialization() throws Exception {
        RpcRequestProcessor rpcRequestProcessor = appContext.getBean(RpcRequestProcessor.class);
        StringRequestRpcInvocation stringInvocation = new StringRequestRpcInvocation("request");
        IntegerRequestRpcInvocation integerInvocation = new IntegerRequestRpcInvocation(5);

        rpcRequestProcessor.accept(stringInvocation);
        rpcRequestProcessor.accept(integerInvocation);

        assertThat(stringInvocation.result, Matchers.is(4));
        assertThat(integerInvocation.result, Matchers.is("4"));
    }

    @Configuration
    @Import(RpcRequestProcessorConfiguration.class)
    public static class MyConfig {
        @Bean
        Nova nova () {
            return Nova.builder().build();
        }
        @Bean
        Handler handler () {
            return new Handler();
        }
    }

    public static class Handler {
        @OnRpcInvocation(String.class)
        public void processA(String request, StringRequestRpcInvocation invocation) {
            invocation.complete(4);
        }

        @OnRpcInvocation(Integer.class)
        public void processB (Integer request, IntegerRequestRpcInvocation invocation) {
            invocation.complete("4");
        }
    }

    public static class StringRequestRpcInvocation extends RpcInvocation<String, IncomingMessageMetaData<?,?>, Integer, Integer> {
        Object result;

        public StringRequestRpcInvocation(String request) {
            super(new IncomingMessage<>(request, new IncomingMessageMetaData<>(new Object(), null)), null, null);
        }

        @Override
        public <T> void complete(T reply, Integer replySpecificInfo, Function<T, Integer> transcriber) throws Exception {
            this.result = reply;
        }
    }

    public static class IntegerRequestRpcInvocation extends RpcInvocation<Integer, IncomingMessageMetaData<?,?>, String, String> {
        Object result;

        public IntegerRequestRpcInvocation(Integer request) {
            super(new IncomingMessage<>(request, new IncomingMessageMetaData<>(new Object(), null)), null, null);
        }

        @Override
        public <T> void complete(T reply, String replySpecificInfo, Function<T, String> transcriber) throws Exception {
            super.complete(reply, replySpecificInfo, transcriber);
            this.result = reply;
        }
    }
}

