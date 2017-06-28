package ch.squaredesk.nova.service.admin.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Error extends AdminMessage {
    public final String message;

    @JsonCreator
    public Error(@JsonProperty("message") String message) {
        this.message = message;
    }
}
