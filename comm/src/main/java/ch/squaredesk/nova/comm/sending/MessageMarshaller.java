package ch.squaredesk.nova.comm.sending;

@FunctionalInterface
public interface MessageMarshaller<T,R> {
    R marshal(T t) throws Exception ;
}
