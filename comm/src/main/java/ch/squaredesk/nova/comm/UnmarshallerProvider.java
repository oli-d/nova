package ch.squaredesk.nova.comm;

import ch.squaredesk.nova.comm.retrieving.MessageUnmarshaller;

@FunctionalInterface
public interface UnmarshallerProvider<T> {
    <U> MessageUnmarshaller<T, U> getUnmarshallerForMessageType(Class<U> tClass);
}
