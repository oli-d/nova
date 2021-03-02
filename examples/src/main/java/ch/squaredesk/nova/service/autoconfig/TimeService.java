/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.service.autoconfig;


import ch.squaredesk.nova.autoconfigure.service.NovaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@SpringBootApplication
public class TimeService implements NovaService, CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(TimeService.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.print("TimeService started, press <ENTER> to shutdown");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            reader.readLine();
        }
    }
}
