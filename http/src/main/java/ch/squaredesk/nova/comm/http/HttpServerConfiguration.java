package ch.squaredesk.nova.comm.http;

public class HttpServerConfiguration {
    public final int port;
    public final String interfaceName;

    public HttpServerConfiguration(String interfaceName, int port) {
        this.port = port;
        this.interfaceName = interfaceName;
    }

    @Override
    public String toString() {
        return "HttpServerConfiguration{" +
                "port=" + port +
                ", interfaceName='" + interfaceName + '\'' +
                '}';
    }
}
