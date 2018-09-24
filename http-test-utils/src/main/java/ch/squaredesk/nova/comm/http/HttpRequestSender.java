/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequestSender {
    private HttpRequestSender() {
    }

    public static HttpResponse sendPutRequest (String url, String request) throws IOException {
        return sendPutRequest(new URL(url), request);
    }

    public static HttpResponse sendPutRequest (URL url, String request) throws IOException {
        return sendRequest("PUT", url, request);
    }

    public static HttpResponse sendPostRequest (String url, String request) throws IOException {
        return sendPostRequest(new URL(url), request);
    }

    public static HttpResponse sendPostRequest (URL url, String request) throws IOException {
        return sendRequest("POST", url, request);
    }

    public static HttpResponse sendGetRequest (String url) throws IOException {
        return sendGetRequest(new URL(url));
    }

    public static HttpResponse sendGetRequest (URL url) throws IOException {
        return sendRequest("GET", url, null);
    }

    public static HttpResponse sendRequest (String method, URL url, String request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        if ("POST".equals(method) || "PUT".equals(method)) {
            connection.setDoOutput(true);
        }
        connection.setRequestMethod(method);
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setRequestProperty("Content-Type", "text/plain");

        if (request!=null) {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(request.getBytes(charset));
            }
        }
        connection.connect();

        StringBuilder sb = new StringBuilder();
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode > 299) {
            if (connection.getErrorStream() != null) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while (errorReader.ready()) {
                    sb.append(errorReader.readLine()).append('\n');
                }
                errorReader.close();
                return new HttpResponse(responseCode, sb.toString().trim());
            } else {
                return new HttpResponse(responseCode, connection.getResponseMessage());
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while (reader.ready()) {
            sb.append(reader.readLine()).append('\n');
        }
        reader.close();

        return new HttpResponse(responseCode, sb.toString().trim());
    }

    public static class HttpResponse {
        public final int returnCode;
        public final String replyMessage;

        public HttpResponse(int returnCode, String replyMessage) {
            this.returnCode = returnCode;
            this.replyMessage = replyMessage;
        }
    }
}
