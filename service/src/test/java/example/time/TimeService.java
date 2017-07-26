/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package example.time;

import ch.squaredesk.nova.service.NovaService;
import org.glassfish.grizzly.http.server.HttpServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TimeService extends NovaService {
    private final HttpServer httpServer;

    public TimeService(HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    @Override
    protected void onShutdown() {
        httpServer.shutdown();
    }

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
}
