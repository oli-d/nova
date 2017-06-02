package ch.squaredesk.nova.service.admin.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Error extends AdminMessage {
    public final String code;
    public final String details;

    @JsonCreator
    public Error(@JsonProperty("code") String code, @JsonProperty("details") String details) {
        this.code = code;
        this.details = details;
    }
}
