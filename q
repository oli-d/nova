[1mdiff --git a/http/src/main/java/ch/squaredesk/nova/comm/http/HttpAdapter.java b/http/src/main/java/ch/squaredesk/nova/comm/http/HttpAdapter.java[m
[1mindex 40845c7..c660ad1 100644[m
[1m--- a/http/src/main/java/ch/squaredesk/nova/comm/http/HttpAdapter.java[m
[1m+++ b/http/src/main/java/ch/squaredesk/nova/comm/http/HttpAdapter.java[m
[36m@@ -136,7 +136,10 @@[m [mpublic class HttpAdapter<MessageType> {[m
     }[m
 [m
     public void shutdown() {[m
[31m-        rpcServer.shutdown();[m
[32m+[m[32m        if (rpcServer!=null) {[m
[32m+[m[32m            rpcServer.shutdown();[m
[32m+[m[32m        }[m
[32m+[m[32m        rpcClient.shutdown();[m
     }[m
 [m
     public static <MessageType> Builder<MessageType> builder(Class<MessageType> messageTypeClass) {[m
[1mdiff --git a/http/src/main/java/ch/squaredesk/nova/comm/http/RpcClient.java b/http/src/main/java/ch/squaredesk/nova/comm/http/RpcClient.java[m
[1mindex 7cc2e53..1fd2a34 100644[m
[1m--- a/http/src/main/java/ch/squaredesk/nova/comm/http/RpcClient.java[m
[1m+++ b/http/src/main/java/ch/squaredesk/nova/comm/http/RpcClient.java[m
[36m@@ -99,4 +99,8 @@[m [mclass RpcClient<InternalMessageType> extends ch.squaredesk.nova.comm.rpc.RpcClie[m
 [m
         return timeoutSingle.ambWith(resultSingle);[m
     }[m
[32m+[m
[32m+[m[32m    void shutdown() {[m
[32m+[m[32m        client.close();[m
[32m+[m[32m    }[m
 }[m
[1mdiff --git a/http/src/test/java/ch/squaredesk/nova/comm/http/HttpAdapterTest.java b/http/src/test/java/ch/squaredesk/nova/comm/http/HttpAdapterTest.java[m
[1mindex 751bd69..f3b4145 100644[m
[1m--- a/http/src/test/java/ch/squaredesk/nova/comm/http/HttpAdapterTest.java[m
[1m+++ b/http/src/test/java/ch/squaredesk/nova/comm/http/HttpAdapterTest.java[m
[36m@@ -13,10 +13,7 @@[m [mpackage ch.squaredesk.nova.comm.http;[m
 [m
 import io.reactivex.observers.TestObserver;[m
 import org.glassfish.grizzly.http.server.HttpServer;[m
[31m-import org.junit.jupiter.api.Assertions;[m
[31m-import org.junit.jupiter.api.BeforeEach;[m
[31m-import org.junit.jupiter.api.Tag;[m
[31m-import org.junit.jupiter.api.Test;[m
[32m+[m[32mimport org.junit.jupiter.api.*;[m
 [m
 import java.math.BigDecimal;[m
 import java.util.concurrent.ExecutionException;[m
[36m@@ -42,6 +39,11 @@[m [mclass HttpAdapterTest {[m
 [m
     }[m
 [m
[32m+[m[32m    @AfterEach[m
[32m+[m[32m    void tearDown() {[m
[32m+[m[32m        sut.shutdown();[m
[32m+[m[32m    }[m
[32m+[m
     @Test[m
     void nullDestinationThrows() {[m
         Throwable t = assertThrows(IllegalArgumentException.class,[m
