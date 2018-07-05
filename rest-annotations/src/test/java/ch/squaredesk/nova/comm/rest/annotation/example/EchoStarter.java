/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.rest.annotation.example;

import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class EchoStarter {
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(EchoConfiguration.class);
        ctx.refresh();

        // at this point, the REST server is started and properly initialized
        
        System.out.println("Echo server started. Press <ENTER> to stop the server...");
        System.in.read();
        ctx.getBean(HttpServer.class).shutdown();
    }
}
