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

package ch.squaredesk.nova.service;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.metrics.MetricsDump;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public abstract class NovaService {
    private Lifeline lifeline = new Lifeline();

    private boolean started = false;

    protected static final Logger logger = LoggerFactory.getLogger(NovaService.class);

    private final LifecycleEventHandlers lifecycleEventHandlers = new LifecycleEventHandlers();
    protected final ServiceDescriptor serviceDescriptor;

    public final Nova nova;


    protected NovaService(Nova nova) {
        this(nova, null);
    }

    protected NovaService(Nova nova, ServiceDescriptor serviceDescriptor) {
        this.nova = Objects.requireNonNull(nova);

        serviceDescriptor = Optional.ofNullable(serviceDescriptor).orElse(new ServiceDescriptor());
        String serviceName = Optional.ofNullable(serviceDescriptor.serviceName)
                                    .orElse(calculateDefaultServiceName());
        String instanceId = Optional.ofNullable(serviceDescriptor.instanceId)
                                    .orElse(UUID.randomUUID().toString());
        boolean enableLifecycle = serviceDescriptor.lifecycleEnabled;
        this.serviceDescriptor = new ServiceDescriptor(serviceName, instanceId, enableLifecycle);

        setAdditionalMetricsInfoIn(nova.metrics, this.serviceDescriptor);
    }

    String calculateDefaultServiceName() {
        String simpleClassName = getClass().getSimpleName();
        int indexOfDollor = simpleClassName.indexOf('$');
        if (indexOfDollor > 0) {
            return simpleClassName.substring(0, indexOfDollor);
        } else {
            return simpleClassName;
        }
    }

    public void registerInitHandler(Runnable initHandler) {
        this.lifecycleEventHandlers.registerInitHandler(initHandler);
    }

    public void registerStartupHandler(Runnable startupHandler) {
        this.lifecycleEventHandlers.registerStartupHandler(startupHandler);
    }

    public void registerShutdownHandler(Runnable shutdownHandler) {
        this.lifecycleEventHandlers.registerShutdownHandler(shutdownHandler);
    }

    private void doInit() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        lifecycleEventHandlers.invokeInitHandlers();
    }

    public void start() {
        if (!serviceDescriptor.lifecycleEnabled) {
            logger.warn("Service lifecycle disabled, the invocation of start() has no effect");
            return;
        }

        doInit();

        if (started) {
            throw new IllegalStateException("service " + serviceDescriptor + " already started");
        }

        lifecycleEventHandlers.invokeStartupHandlers();

        lifeline.start();
        started = true;
        logger.info("Service {} up and running.", serviceDescriptor);
    }

    public void shutdown() {
        if (!serviceDescriptor.lifecycleEnabled) {
            logger.warn("Service lifecycle disabled, the invocation of shutdown() has no effect");
            return;
        }

        if (started) {
            logger.info("Service {} is shutting down...", serviceDescriptor);
            try {
                lifecycleEventHandlers.invokeShutdownHandlers();
            } catch (Exception e) {
                logger.warn("Error in shutdown procedure of service {}.", serviceDescriptor,e);
            }

            lifeline.cut();
            lifeline = new Lifeline();
            started = false;

            logger.info("Shutdown procedure completed for service {}.", serviceDescriptor);
        }
    }

    public boolean isStarted() {
        return started;
    }

    private static void setAdditionalMetricsInfoIn(Metrics metrics, ServiceDescriptor serviceDescriptor) {
        try {
            InetAddress myInetAddress = InetAddress.getLocalHost();
            metrics.addAdditionalInfoForDumps("hostName", myInetAddress.getHostName());
            metrics.addAdditionalInfoForDumps("hostAddress", myInetAddress.getHostAddress());
        } catch (Exception ex) {
            logger.warn("Unable to determine my IP address. MetricDumps will be lacking this information.");
            metrics.addAdditionalInfoForDumps("hostName", "n/a");
            metrics.addAdditionalInfoForDumps("hostAddress", "n/a");
        }

        metrics.addAdditionalInfoForDumps("serviceName", serviceDescriptor.serviceName);

        if (serviceDescriptor.instanceId != null) {
            metrics.addAdditionalInfoForDumps("serviceInstanceId", serviceDescriptor.instanceId);
            String serviceInstanceName = serviceDescriptor.serviceName + "." + serviceDescriptor.instanceId;
            metrics.addAdditionalInfoForDumps("serviceInstanceName", serviceInstanceName);
        }
    }

    public Flowable<MetricsDump> dumpMetricsContinuously (Duration interval) {
        return nova.metrics.dumpContinuously(interval);
    }

    private class Lifeline extends Thread {
        private final CountDownLatch shutdownLatch = new CountDownLatch(1);
        private Lifeline() {
            super("Lifeline");
        }

        @Override
        public void run() {
            try {
                shutdownLatch.await();
            } catch (InterruptedException e) {
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            }
        }

        void cut() {
            shutdownLatch.countDown();
        }
    }

}
