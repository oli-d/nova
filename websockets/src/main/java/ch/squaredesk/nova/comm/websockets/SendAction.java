package ch.squaredesk.nova.comm.websockets;

public interface SendAction {
    <T> void accept (T message) throws Exception;
}
