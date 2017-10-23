/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets;

public enum CloseReason {
    NORMAL_CLOSURE(1000, "Normal closure"),
    GOING_AWAY(1001, "Endpoint going away"),
    PROTOCOL_ERROR(1002, "Protocol error"),
    CANNOT_ACCEPT(1003, "Unacceptable data received"),
    // RESERVED(1004, "reserved"),
    NO_STATUS_CODE(1005, "No status code", false),
    CLOSED_ABNORMALLY(1006, "Closed abnormally", false),
    NOT_CONSISTENT(1007, "Inconsistent data received"),
    VIOLATED_POLICY(1008, "Policy violating data received"),
    TOO_BIG(1009, "Received data, too big to process"),
    NO_EXTENSION(1010, "No extension"),
    UNEXPECTED_CONDITION(1011, "Unexpected condition"),
    SERVICE_RESTART(1012, "Service is being restarted"),
    TRY_AGAIN_LATER(1013, "Service overloaded"),
    TLS_HANDSHAKE_FAILURE(1015, "TLS handshake failed")
    ;

    public final int code;
    public final String text;
    public final boolean mightBeUsedByEndpoint;

    CloseReason (int code, String text) {
        this(code, text, true);
    }

    CloseReason (int code, String text, boolean mightBeUsedByEndpoint) {
        this.code = code;
        this.text = text;
        this.mightBeUsedByEndpoint = mightBeUsedByEndpoint;
    }

    public static CloseReason forCloseCode (int code) {
        switch (code) {
            case 1000: return NORMAL_CLOSURE;
            case 1001: return GOING_AWAY;
            case 1002: return PROTOCOL_ERROR;
            case 1003: return CANNOT_ACCEPT;
            case 1005: return NO_STATUS_CODE;
            case 1006: return CLOSED_ABNORMALLY;
            case 1007: return NOT_CONSISTENT;
            case 1008: return VIOLATED_POLICY;
            case 1009: return TOO_BIG;
            case 1010: return NO_EXTENSION;
            case 1011: return UNEXPECTED_CONDITION;
            case 1012: return SERVICE_RESTART;
            case 1013: return TRY_AGAIN_LATER;
            case 1015: return TLS_HANDSHAKE_FAILURE;
            default: throw new RuntimeException("Unsupported close code " + code);
        }
    }
}
