package ch.squaredesk.nova.comm.rest;

public class RestServerConfiguration {
    public final int port;
    public final String interfaceName;

    public RestServerConfiguration(int port, String interfaceName) {
        this.port = port;
        this.interfaceName = interfaceName;
    }

    @Override
    public String toString() {
        return "RestServerConfiguration{" +
                "port=" + port +
                ", interfaceName='" + interfaceName + '\'' +
                '}';
    }
}
