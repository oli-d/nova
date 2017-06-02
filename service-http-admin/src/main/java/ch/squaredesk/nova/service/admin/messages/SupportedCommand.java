package ch.squaredesk.nova.service.admin.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SupportedCommand {
    public final String url;
    public final String[] parameters;

    @JsonCreator
    public SupportedCommand(@JsonProperty("url") String url, @JsonProperty("parameters") String... parameters) {
        this.url = url;
        this.parameters = parameters;
    }
}
