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

package ch.squaredesk.nova.comm.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HttpRequestSender {
    public static Duration defaultTimeout = Duration.ofSeconds(30);

    private HttpRequestSender() {
    }

    public static HttpResponse sendPutRequest (String url, String request) throws IOException {
        return sendPutRequest(new URL(url), request, null);
    }

    public static HttpResponse sendPutRequest (String url, String request, RequestHeaders requestHeaders) throws IOException {
        return sendPutRequest(new URL(url), request, requestHeaders);
    }

    public static HttpResponse sendPutRequest (URL url, String request) throws IOException {
        return sendPutRequest( url, request, null);
    }

    public static HttpResponse sendPutRequest (URL url, String request, RequestHeaders requestHeaders) throws IOException {
        return sendRequest("PUT", url, request, requestHeaders);
    }

    public static HttpResponse sendPostRequest (String url, String request) throws IOException {
        return sendPostRequest(new URL(url), request, null);
    }

    public static HttpResponse sendPostRequest (String url, String request, RequestHeaders requestHeaders) throws IOException {
        return sendPostRequest(new URL(url), request, requestHeaders);
    }

    public static HttpResponse sendPostRequest (URL url, String request) throws IOException {
        return sendPostRequest( url, request, null);
    }

    public static HttpResponse sendPostRequest (URL url, String request, RequestHeaders requestHeaders) throws IOException {
        return sendRequest("POST", url, request, requestHeaders);
    }

    public static HttpResponse sendGetRequest (String url) throws IOException {
        return sendGetRequest(new URL(url));
    }

    public static HttpResponse sendGetRequest (URL url) throws IOException {
        return sendRequest("GET", url, null, null);
    }

    public static HttpResponse sendGetRequest (String url, RequestHeaders requestHeaders) throws IOException {
        return sendGetRequest(new URL(url), requestHeaders);
    }

    public static HttpResponse sendGetRequest (URL url, RequestHeaders requestHeaders) throws IOException {
        return sendRequest("GET", url, null, requestHeaders);
    }

    public static HttpResponse sendOptionsRequest (String url) throws IOException {
        return sendOptionsRequest(new URL(url));
    }

    public static HttpResponse sendOptionsRequest (URL url) throws IOException {
        return sendOptionsRequest(url, null);
    }

    public static HttpResponse sendOptionsRequest (String url, RequestHeaders requestHeaders) throws IOException {
        return sendOptionsRequest(new URL(url), requestHeaders);
    }

    public static HttpResponse sendOptionsRequest (URL url, RequestHeaders requestHeaders) throws IOException {
        return sendRequest("OPTIONS", url, null, requestHeaders);
    }

    public static HttpResponse sendRequest (String method, URL url, String request, RequestHeaders requestHeaders) throws IOException {
        return sendRequest(method, url, request, requestHeaders, defaultTimeout);
    }

    public static HttpResponse sendRequest (String method, URL url, String request, RequestHeaders requestHeaders, Duration timeout) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        if ("POST".equals(method) || "PUT".equals(method)) {
            connection.setDoOutput(true);
        }
        connection.setRequestMethod(method);
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);
        if (requestHeaders != null) {
            requestHeaders.entrySet().forEach(entry -> connection.setRequestProperty(entry.getKey(), entry.getValue()));
        }

        if (request!=null) {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(request.getBytes(charset));
            }
        }

        connection.setReadTimeout((int)timeout.toMillis());
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

    public static class RequestHeaders {
        private final Map<String, String> map = new HashMap<>();

        private RequestHeaders() {
        }

        public static RequestHeaders create(String headerName, String value) {
            return new RequestHeaders().add(headerName, value);
        }

        public static RequestHeaders createForContentType(String contentType) {
            return new RequestHeaders().addContentType(contentType);
        }

        public RequestHeaders addContentType (String contentType) {
            map.put("Content-Type", contentType);
            return this;
        }

        public RequestHeaders add (String headerName, String value) {
            map.put(headerName, value);
            return this;
        }

        public Set<Map.Entry<String, String>> entrySet() {
            return map.entrySet();
        }
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
