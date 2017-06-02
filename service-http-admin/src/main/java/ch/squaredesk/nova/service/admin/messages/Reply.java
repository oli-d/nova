package ch.squaredesk.nova.service.admin.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Reply extends AdminMessage {
    public final String details;

    @JsonCreator
    public Reply(@JsonProperty("details") String details) {
        this.details = details;
    }
}
