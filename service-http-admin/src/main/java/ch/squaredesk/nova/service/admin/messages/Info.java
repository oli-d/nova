package ch.squaredesk.nova.service.admin.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Info extends AdminMessage {
    public final SupportedCommand[] supportedCommands;

    @JsonCreator
    public Info(@JsonProperty("supportedCommands") SupportedCommand... supportedCommands) {
        this.supportedCommands = supportedCommands;
    }


}
