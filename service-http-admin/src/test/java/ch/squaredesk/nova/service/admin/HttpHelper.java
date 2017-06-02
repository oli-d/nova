package ch.squaredesk.nova.service.admin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
                Socket socket = new Socket("0.0.0.0", port);
                return;
            } catch (Exception e) {
                TimeUnit.MILLISECONDS.sleep(50);
            }
        }
        throw new RuntimeException("Nobody listening on port " + port);
    }

    public static String getResponseBody (String url, String request) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true); // Triggers POST.
        String charset = "UTF-8";
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(request.getBytes(charset));
        }
        connection.connect();

        StringBuilder sb = new StringBuilder();
        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();
        if (responseCode < 200 || responseCode > 299) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            while (errorReader.ready()) {
                sb.append(errorReader.readLine()).append('\n');
            }
            errorReader.close();
            return sb.toString().trim();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while (reader.ready()) {
            sb.append(reader.readLine()).append('\n');
        }
        reader.close();

        return  sb.toString().trim();
    }
}
