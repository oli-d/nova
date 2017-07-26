/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service;

import ch.squaredesk.nova.Nova;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public abstract class NovaService {
    private Lifeline lifeline = new Lifeline();

    private boolean started = false;

    protected final Logger logger;

    @Autowired
    protected Nova nova;
    @Autowired
    protected String instanceId;
    @Autowired
    protected String serviceName;

    protected NovaService() {
        this.logger = LoggerFactory.getLogger(getClass());
    }

    @PostConstruct
    private void doInit() {
        Objects.requireNonNull(nova);

        try {
            onInit();
        } catch (Throwable t) {
            logger.error("Unable to initialize service " + serviceName,t);
            throw new RuntimeException(t);
        }
    }

    public void start() {
        if (started) {
            throw new IllegalStateException("service " + serviceName + "/" + instanceId + " already started");
        }

        try {
            onStart();
        } catch (Throwable t) {
            logger.error("Unable to initialize service " + serviceName, t);
            throw new RuntimeException("unable to start service due to uncaught exception", t);
        }

        lifeline.start();
        started = true;
        logger.info("Service {}, instance {} up and running.", serviceName, instanceId);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Service {}, instance {} is shutting down...", serviceName, instanceId);
        try {
            onShutdown();
        } catch (Exception e) {
            logger.warn("Error in shutdown procedure of instance " + instanceId,e);
        }

        lifeline.cut();
        lifeline = new Lifeline();
        started = false;

        logger.info("Shutdown procedure completed for service {}, instance {}.", serviceName, instanceId);
    }

    /**
     * Extension point for sub classes
     */
    protected void onInit() throws Exception {
    }

    /**
     * Extension point for sub classes
     */
    protected void onStart() throws Exception {
    }

    /**
     * Extension point for sub classes
     */
    protected void onShutdown() {
    }

    public boolean isStarted() {
        return started;
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

        public void cut() {
            shutdownLatch.countDown();
        }
    }

    private static void assertIsAnnotated(Class classToCheck, Class annotationToCheckFor) {
        boolean annotated = Arrays.stream(classToCheck.getAnnotations())
                .anyMatch(anno -> anno.annotationType().equals(annotationToCheckFor));
        if (!annotated) {
            throw new IllegalArgumentException("the class " + classToCheck.getName() + " must be annotated with @" +
                    annotationToCheckFor.getSimpleName());
        }
    }

    private static void assertIsAnnotated(Class classToCheck, String methodToFind, Class annotationToCheckFor) {
        Method methodToCheck;
        try {
            methodToCheck = classToCheck.getMethod(methodToFind);
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate service", e);
        }

        boolean annotated = Arrays.stream(methodToCheck.getAnnotations())
                .anyMatch(anno -> anno.annotationType().equals(annotationToCheckFor));
        if (!annotated) {
            throw new IllegalArgumentException("the method " + methodToFind + "() must be annotated with @" +
                    annotationToCheckFor.getSimpleName());
        }
    }

    public static <T extends NovaService, U extends NovaServiceConfiguration<T>>
        T createInstance(Class<T> serviceClass, Class<U> configurationClass) {

        // ensure, that the passed config class is properly annotated
        assertIsAnnotated(configurationClass, Configuration.class);

        // and, that we also have an appropriate service bean creator
        assertIsAnnotated(configurationClass, "serviceInstance", Bean.class);

        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext();

        ctx.register(configurationClass);
        ctx.refresh();

        return ctx.getBean(serviceClass);
    }

}
