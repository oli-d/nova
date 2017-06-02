package ch.squaredesk.nova.service.admin.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS,
                include = JsonTypeInfo.As.PROPERTY,
                property = "type")
public class AdminMessage {
    public final long timestamp = System.currentTimeMillis();
}
