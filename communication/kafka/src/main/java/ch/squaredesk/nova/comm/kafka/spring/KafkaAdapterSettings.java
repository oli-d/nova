package ch.squaredesk.nova.comm.kafka.spring;

public class KafkaAdapterSettings {
    public final String identifier;
    public final String serverAddress;
    public final long pollTimeoutInMilliseconds;

    public KafkaAdapterSettings(String identifier, String serverAddress, long pollTimeoutInMilliseconds) {
        this.identifier = identifier;
        this.serverAddress = serverAddress;
        this.pollTimeoutInMilliseconds = pollTimeoutInMilliseconds;
    }
}
