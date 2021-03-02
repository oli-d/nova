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

import ch.squaredesk.nova.Nova;
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
    Nova nova;

    @Override
    public void afterPropertiesSet() {
        try {
            InetAddress myInetAddress = InetAddress.getLocalHost();
            nova.metrics.addAdditionalInfoForDumps("hostName", myInetAddress.getHostName());
            nova.metrics.addAdditionalInfoForDumps("hostAddress", myInetAddress.getHostAddress());
        } catch (Exception ex) {
            logger.warn("Unable to determine my IP address. MetricDumps will be lacking this information.");
            nova.metrics.addAdditionalInfoForDumps("hostName", "n/a");
            nova.metrics.addAdditionalInfoForDumps("hostAddress", "n/a");
        }

        Optional.ofNullable(serviceDescriptor)
                .ifPresent(sd -> {
                    nova.metrics.addAdditionalInfoForDumps("serviceName", sd.serviceName());
                    nova.metrics.addAdditionalInfoForDumps("serviceInstanceId", serviceDescriptor.instanceId());
                    String serviceInstanceName = serviceDescriptor.serviceName() + "." + serviceDescriptor.instanceId();
                    nova.metrics.addAdditionalInfoForDumps("serviceInstanceName", serviceInstanceName);

                });
    }
}
