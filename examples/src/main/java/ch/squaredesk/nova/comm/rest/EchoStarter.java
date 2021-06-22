/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EchoStarter implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoStarter.class);

    public static void main(String[] args) {
        SpringApplication.run(EchoStarter.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("Echo server started. Press <ENTER> to stop the server...");
        System.in.read();
        LOGGER.info("Shutting down...");
    }

}
