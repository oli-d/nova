package ch.squaredesk.nova.comm.retrieving;

@FunctionalInterface
public interface MessageUnmarshaller<T,R> {
    R unmarshal (T t) throws Exception ;
}
