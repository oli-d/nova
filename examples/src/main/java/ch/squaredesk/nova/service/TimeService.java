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


import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TimeService /* extends NovaService */ {
    // @OnServiceInit
    public void sayHello() {
        System.out.println("Initializing the TimeServer...");
    }
/*
    public static void main(String[] args) {
        TimeService time = NovaService.createInstance(TimeService.class, TimeServiceConfig.class);
        time.start();
        System.out.printf("TimeService started, press <ENTER> to shutdown");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        time.shutdown();
    }

 */
}
