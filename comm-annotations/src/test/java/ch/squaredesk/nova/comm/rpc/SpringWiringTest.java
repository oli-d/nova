package ch.squaredesk.nova.comm.rpc;

public class SpringWiringTest {

    public static class MyServiceBean {
        @RpcInvocationSource
        public final Object myCommAdapter;

        public MyServiceBean(Object myCommAdapter) {
            this.myCommAdapter = myCommAdapter;
        }
    }
}
