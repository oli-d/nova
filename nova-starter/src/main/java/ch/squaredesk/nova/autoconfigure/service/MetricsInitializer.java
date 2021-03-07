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

package ch.squaredesk.nova.autoconfigure.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.util.Optional;

public class MetricsInitializer implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(MetricsInitializer.class);

    @Autowired(required = false)
    ServiceDescriptor serviceDescriptor;

    @Autowired
    MeterRegistry meterRegistry;

    @Override
    public void afterPropertiesSet() {
        try {
            InetAddress myInetAddress = InetAddress.getLocalHost();
            meterRegistry.config().commonTags(
                    "hostName", myInetAddress.getHostName(),
                    "hostAddress", myInetAddress.getHostAddress());
        } catch (Exception ex) {
            logger.warn("Unable to determine my IP address. MetricDumps will be lacking this information.");
        }

        Optional.ofNullable(serviceDescriptor)
                .ifPresent(sd -> {
                    String serviceInstanceName = serviceDescriptor.serviceName() + "." + serviceDescriptor.instanceId();
                    meterRegistry.config().commonTags(
                            "serviceName", sd.serviceName(),
                            "serviceInstanceId", serviceDescriptor.instanceId(),
                            "serviceInstanceName", serviceInstanceName);

                });
    }
}
