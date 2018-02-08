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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RpcRequestProcessorConfiguration {
    @Bean
    public RpcRequestProcessor rpcRequestProcessor() {
        return new RpcRequestProcessor();
    }

    @Bean
    public RpcRequestHandlingBeanPostprocessor rpcRequestHandlingBeanPostprocessor() {
        return new RpcRequestHandlingBeanPostprocessor();
    }

}
