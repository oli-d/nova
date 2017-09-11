/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.metrics.elastic;

import ch.squaredesk.nova.metrics.CompoundMetric;
import ch.squaredesk.nova.metrics.Metrics;
import ch.squaredesk.nova.metrics.MetricsDump;
import ch.squaredesk.nova.tuples.Pair;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.functions.Consumer;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkAction;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElasticMetricsReporterTest {
    private ElasticMetricsReporter sut;

    @BeforeEach
    void setup() {
        sut = new ElasticMetricsReporter("127.0.0.1",9300,"cluster","index");
    }

    @Test
    void nothingHappensOnShutdownIfNotConnected() {
        sut.shutdown();
    }

    @Test
    void shutdownClosesConnection() throws Exception {
        TransportClient client = injectTransportClientMockIntoSut();

        sut.shutdown();

        Mockito.verify(client).close();
        Mockito.verifyNoMoreInteractions(client);
    }

    @Test
    void transmittingThrowsIfNotStartedYet() {
        Metrics metrics = new Metrics();
        metrics.getCounter("test", "counter1");
        metrics.getMeter("test", "meter1");
        metrics.register(new MyMetric(), "test", "myMetric1");
        MetricsDump dump = new MetricsDump(metrics.getMetrics());

        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                () -> sut.accept(dump)
                );
        assertThat(ex.getMessage(), is("not started yet"));
    }

    @Test
    // Note that running this dumps out a stack trace. This is ok, since there is no Elastic to talk to
    // Important thing is that we inspect the request we have built and which *would* be sent
    void novaDumpIsTransmittedAsExpected() throws Exception {
        TransportClient client = injectTransportClientMockIntoSut();
        BulkRequestBuilder brb = new BulkRequestBuilder(client, BulkAction.INSTANCE);
        Mockito.when(client.prepareBulk()).thenReturn(brb);

        Metrics metrics = new Metrics();
        metrics.getCounter("test","counter1");
        metrics.getMeter("test","meter1");
        metrics.register(new MyMetric(), "test","myMetric1");
        MetricsDump dump = new MetricsDump(metrics.getMetrics());

        sut.accept(dump);

        Mockito.verify(client).prepareBulk();
        Mockito.verifyNoMoreInteractions(client);

        List<ActionRequest> requests = brb.request().requests();
        assertThat(requests.size(), is(3));
        for (ActionRequest request: requests) {
            assertTrue(request instanceof IndexRequest);
            IndexRequest ir = (IndexRequest)request;
            Map<String,Object> sourceAsMap = getMapFrom(ir.source());
            assertNotNull(sourceAsMap.get("host"));
            assertNotNull(sourceAsMap.get("hostAddress"));
            assertNotNull(sourceAsMap.get("@timestamp"));
            assertThat(sourceAsMap.get("name"),Matchers.oneOf("test.counter1","test.meter1","test.myMetric1"));
        };
    }

    @Test
    void mapDumpMissingTypeInfoCausesError() throws Exception {
        TransportClient client = injectTransportClientMockIntoSut();
        BulkRequestBuilder brb = new BulkRequestBuilder(client, BulkAction.INSTANCE);
        Mockito.when(client.prepareBulk()).thenReturn(brb);

        Map<String, Object> dumpAsMap = new HashMap<>();
        Arrays.asList("counter1", "meter1", "myMetric1")
                .forEach(name -> {
                    Map<String, Object> metricMap = new HashMap<>();
                    metricMap.put("host", "someVal");
                    metricMap.put("someAttribute", "someVal");
                    dumpAsMap.put("test." + name, metricMap);
                });

        Throwable[] exceptionHolder = new Throwable[1];
        CountDownLatch cdl = new CountDownLatch(1);
        Consumer<Throwable> exceptionHandler = throwable -> {
            exceptionHolder[0] = throwable;
            cdl.countDown();
        };
        sut.accept(dumpAsMap, exceptionHandler);

        cdl.await(30, TimeUnit.SECONDS);
        assertThat(cdl.getCount(), is(0L));
        assertThat(exceptionHolder[0].getMessage(), is("metricMap must contain type entry"));

        Mockito.verify(client).prepareBulk();
        Mockito.verifyNoMoreInteractions(client);

        List<ActionRequest> requests = brb.request().requests();
        assertThat(requests.size(), is(0));
    }

    @Test
    // Note that running this dumps out a stack trace. This is ok, since there is no Elastic to talk to
    // Important thing is that we inspect the request we have built and which *would* be sent
    void mapDumpIsTransmittedAsExpected() throws Exception {
        TransportClient client = injectTransportClientMockIntoSut();
        BulkRequestBuilder brb = new BulkRequestBuilder(client, BulkAction.INSTANCE);
        Mockito.when(client.prepareBulk()).thenReturn(brb);

        Map<String, Object> dumpAsMap = new HashMap<>();
        Arrays.asList(new Pair<>("counter", "counter1"),
                new Pair<>("meter", "meter1"),
                new Pair<>("MyMetric", "myMetric1"))
                .forEach(pair -> {
                    Map<String, Object> metricMap = new HashMap<>();
                    metricMap.put("type", pair._1);
                    metricMap.put("someAttribute", "someVal");
                    dumpAsMap.put("test." + pair._2, metricMap);
                });

        dumpAsMap.put("hostName", "someVal");
        sut.accept(dumpAsMap);

        Mockito.verify(client).prepareBulk();
        Mockito.verifyNoMoreInteractions(client);

        List<ActionRequest> requests = brb.request().requests();
        assertThat(requests.size(), is(3));
        for (ActionRequest request : requests) {
            assertTrue(request instanceof IndexRequest);
            IndexRequest ir = (IndexRequest) request;
            assertNotNull(ir.type());
            assertThat(ir.type(), Matchers.oneOf("counter", "meter", "MyMetric"));
            Map<String, Object> sourceAsMap = getMapFrom(ir.source());
            assertNotNull(sourceAsMap.get("host"));
            assertNotNull(sourceAsMap.get("someAttribute"));
            assertThat(sourceAsMap.get("name"), Matchers.oneOf("test.counter1", "test.meter1", "test.myMetric1"));
        }
    }

    private Map<String,Object> getMapFrom (BytesReference source) throws Exception {
        return new ObjectMapper().readValue(source.utf8ToString(), Map.class);
    }

    private TransportClient injectTransportClientMockIntoSut() throws Exception {
        TransportClient client = Mockito.mock(TransportClient.class);
        Field f = sut.getClass().getDeclaredField("client");
        f.setAccessible(true);
        f.set(sut,client);
        return client;
    }

    private class MyMetric implements CompoundMetric {
        @Override
        public Map<String,Object> getValues() {
            return new HashMap<>();
        }
    }
}