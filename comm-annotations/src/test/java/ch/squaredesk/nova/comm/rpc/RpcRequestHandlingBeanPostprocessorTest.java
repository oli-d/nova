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

import ch.squaredesk.nova.tuples.Pair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.junit.MatcherAssert.assertThat;

class RpcRequestHandlingBeanPostprocessorTest {
    private ApplicationContext appContext;
    private RpcRequestProcessor rpcRequestProcessor;

    @BeforeEach
    void setUp() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(MyConfig.class);
        ctx.refresh();
        appContext = ctx;
    }

    @Test
    void rpcRequestProcessorAutomaticallyWiredUpDuringSpringContextInitialization() throws Exception {
        rpcRequestProcessor = appContext.getBean(RpcRequestProcessor.class);
        StringRequestRpcInvocation stringInvocation = new StringRequestRpcInvocation("request");
        IntegerRequestRpcInvocation integerInvocation = new IntegerRequestRpcInvocation(5);

        Pair pairStringRequest = rpcRequestProcessor.apply(stringInvocation);
        Pair pairIntRequest = rpcRequestProcessor.apply(integerInvocation);

        assertThat(pairStringRequest._1, sameInstance(stringInvocation));
        assertThat(pairStringRequest._2, Matchers.is(4));
        assertThat(pairIntRequest._1, sameInstance(integerInvocation));
        assertThat(pairIntRequest._2, Matchers.is("4"));
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
        public int processA(String s) {
            return 4;
        }

        @OnRpcInvocation(Integer.class)
        public String processB (Integer i) {
            return "4";
        }
    }

    public static class StringRequestRpcInvocation extends RpcInvocation<String, String, Integer, Integer> {
        public StringRequestRpcInvocation(String request) {
            super(request, null, null, null);
        }
    }

    public static class IntegerRequestRpcInvocation extends RpcInvocation<Integer, Integer, String, String> {
        public IntegerRequestRpcInvocation(Integer request) {
            super(request, null, null, null);
        }
    }
}

