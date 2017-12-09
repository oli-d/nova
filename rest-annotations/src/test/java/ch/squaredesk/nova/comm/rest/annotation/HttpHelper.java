/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.rest.annotation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class HttpHelper {
    public static synchronized int nextFreePort() throws Exception {
        ServerSocket ss = new ServerSocket(0);
        ss.close();
        int port = ss.getLocalPort();
        return port;
    }

    public static void waitUntilSomebodyListensOnPort(int port, long timeout, TimeUnit timeUnit) throws Exception {
        boolean connected = false;
        long maxTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (!connected && System.currentTimeMillis() < maxTime) {
            try {
                Socket socket = new Socket("localhost", port);
                return;
            } catch (Exception e) {
                TimeUnit.MILLISECONDS.sleep(50);
            }
        }
        throw new RuntimeException("Nobody listening on port " + port);
    }

    public static String getResponseBody (String url, String request) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
//        connection.setDoOutput(true); // Triggers POST.
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setRequestProperty("Content-Type", "text/plain");

//        try (OutputStream output = connection.getOutputStream()) {
//            output.write(request.getBytes(charset));
//        }
        connection.connect();

        StringBuilder sb = new StringBuilder();
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode > 299) {
            if (connection.getErrorStream()!= null) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while (errorReader.ready()) {
                    sb.append(errorReader.readLine()).append('\n');
                }
                errorReader.close();
                return sb.toString().trim();
            } else {
                return "" + responseCode + " " + connection.getResponseMessage();
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while (reader.ready()) {
            sb.append(reader.readLine()).append('\n');
        }
        reader.close();

        return  sb.toString().trim();
    }
}
