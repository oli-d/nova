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
import ch.squaredesk.nova.metrics.MetricsDump;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class NovaService {
    private Lifeline lifeline = new Lifeline();

    private boolean started = false;

    protected final Logger logger;

    final boolean registerShutdownHook;

    private final LifecycleEventHandlers lifecycleEventHandlers = new LifecycleEventHandlers();
    protected final String instanceId;
    protected final String serviceName;

    public final Nova nova;


    protected NovaService(Nova nova) {
        this(nova, null);
    }

    protected NovaService(Nova nova, NovaServiceConfig novaServiceConfig) {
        this.nova = Objects.requireNonNull(nova);
        this.logger = LoggerFactory.getLogger(getClass());
        this.serviceName = Optional.ofNullable(novaServiceConfig)
                                    .flatMap(cfg -> Optional.ofNullable(cfg.serviceName))
                                    .orElse("");
        this.instanceId = Optional.ofNullable(novaServiceConfig)
                                    .flatMap(cfg -> Optional.ofNullable(cfg.instanceId))
                                    .orElse(UUID.randomUUID().toString());
        this.registerShutdownHook = Optional.ofNullable(novaServiceConfig)
                                    .flatMap(cfg -> Optional.ofNullable(cfg.registerShutdownHook))
                                    .orElse(true);
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
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        }

        calculateAdditionalInfoForMetricsDump();

        lifecycleEventHandlers.invokeInitHandlers();
    }

    public void start() {
        doInit();

        if (started) {
            throw new IllegalStateException("service " + serviceName + "/" + instanceId + " already started");
        }

        lifecycleEventHandlers.invokeStartupHandlers();

        lifeline.start();
        started = true;
        logger.info("Service {}, instance {} up and running.", serviceName, instanceId);
    }

    public void shutdown() {
        if (started) {
            logger.info("Service {}, instance {} is shutting down...", serviceName, instanceId);
            try {
                lifecycleEventHandlers.invokeShutdownHandlers();
            } catch (Exception e) {
                logger.warn("Error in shutdown procedure of instance " + instanceId,e);
            }

            lifeline.cut();
            lifeline = new Lifeline();
            started = false;

            logger.info("Shutdown procedure completed for service {}, instance {}.", serviceName, instanceId);
        }
    }

    public boolean isStarted() {
        return started;
    }

    private void calculateAdditionalInfoForMetricsDump() {
        try {
            InetAddress myInetAddress = InetAddress.getLocalHost();
            nova.metrics.addAdditionalInfoForDumps("hostName", myInetAddress.getHostName());
            nova.metrics.addAdditionalInfoForDumps("hostAddress", myInetAddress.getHostAddress());
        } catch (Exception ex) {
            logger.warn("Unable to determine my IP address. MetricDumps will be lacking this information.");
            nova.metrics.addAdditionalInfoForDumps("hostName", "n/a");
            nova.metrics.addAdditionalInfoForDumps("hostAddress", "n/a");
        }

        String serviceNameForMetrics = this.serviceName;
        if (serviceNameForMetrics == null) {
            serviceNameForMetrics = getClass().getSimpleName();
            logger.info("The service name was not provided, so for the metric dumps we derived it from the class name: {} ", serviceNameForMetrics);
        }
        nova.metrics.addAdditionalInfoForDumps("serviceName", serviceNameForMetrics);
        if (instanceId != null) {
            nova.metrics.addAdditionalInfoForDumps("serviceInstanceId", instanceId);
            String serviceInstanceName = serviceName + "." + instanceId;
            nova.metrics.addAdditionalInfoForDumps("serviceInstanceName", serviceInstanceName);
        }
    }

    public Flowable<MetricsDump> dumpMetricsContinuously (long interval, TimeUnit timeUnit) {
        return nova.metrics.dumpContinuously(interval, timeUnit);
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
                // noop
            }
        }

        void cut() {
            shutdownLatch.countDown();
        }
    }

}
