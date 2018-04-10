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
        Handler handler () {
            return new Handler();
        }
    }

    public static class Handler {
        @OnRpcInvocation(String.class)
        public void processA(StringRequestRpcInvocation invocation) {
            invocation.complete(4);
        }

        @OnRpcInvocation(Integer.class)
        public void processB (IntegerRequestRpcInvocation invocation) {
            invocation.complete("4");
        }
    }

    public static class StringRequestRpcInvocation extends RpcInvocation<String, String, Integer, Integer> {
        Integer result;

        public StringRequestRpcInvocation(String request) {
            super(request, null, null, null);
        }

        @Override
        public void complete(Integer result) {
            this.result = result;
        }
    }

    public static class IntegerRequestRpcInvocation extends RpcInvocation<Integer, Integer, String, String> {
        String result;

        public IntegerRequestRpcInvocation(Integer request) {
            super(request, null, null, null);
        }

        @Override
        public void complete(String result) {
            this.result = result;
        }
    }
}

