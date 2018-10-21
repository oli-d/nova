package ch.squaredesk.nova.comm;

import ch.squaredesk.nova.comm.sending.MessageMarshaller;

@FunctionalInterface
public interface MarshallerProvider<U> {
    <T> MessageMarshaller<T, U> getMarshallerForMessageType( Class<T> tClass);
}
