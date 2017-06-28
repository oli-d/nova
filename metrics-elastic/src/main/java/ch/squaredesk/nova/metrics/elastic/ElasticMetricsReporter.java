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
import ch.squaredesk.nova.metrics.MetricsDump;
import com.codahale.metrics.Metric;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;

public class ElasticMetricsReporter implements Consumer<MetricsDump> {
    private static final Logger logger = LoggerFactory.getLogger(ElasticMetricsReporter.class);

    private final String elasticServer;
    private final int elasticPort;
    private final String clusterName;
    private final String indexName;
    private final Map<String, Object> additionalMetricAttributes;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ZoneId zoneForTimestamps = ZoneId.of("UTC");
    private TransportClient client;

    public ElasticMetricsReporter(String elasticServer, int elasticPort, String clusterName, String indexName) {
        this(elasticServer, elasticPort, clusterName, indexName, Collections.EMPTY_MAP);
    }

    public ElasticMetricsReporter(String elasticServer, int elasticPort, String clusterName, String indexName, Map<String, Object> additionalMetricAttributes) {
        this.elasticServer = elasticServer;
        this.elasticPort = elasticPort;
        this.clusterName = clusterName;
        this.indexName = indexName;
        this.additionalMetricAttributes = additionalMetricAttributes;
    }

    public void startup() {
        logger.info("Connecting to Elasticsearch @ " + elasticServer + ":" + elasticPort);
        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .build();

        try {
            client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(elasticServer), elasticPort));
        } catch (UnknownHostException e) {
            logger.error("Unable to connect to Elastic @ " + elasticServer + ":" + elasticPort, e);
        }
        logger.info("\tsuccessfully established connection to Elastic @ " + elasticServer + ":" + elasticPort + " :-)");
    }

    public void shutdown() {
        if (client!=null) {
            logger.info("Shutting down connection to Elasticsearch");
            client.close();
            logger.info("\tsuccessfully shutdown connection to Elasticsearch :-)");
        }
    }

    @Override
    public void accept(MetricsDump metricsDump) throws Exception {
        if (client==null) {
            throw new IllegalStateException("not started yet");
        }

        LocalDateTime timestampInUtc = Instant.ofEpochMilli(metricsDump.timestamp).atZone(zoneForTimestamps).toLocalDateTime();

        Observable.<TypeAndNameAndMetric>create(s -> {
            metricsDump.metrics.entrySet().forEach(entry -> {
                String type = entry.getValue().getClass().getSimpleName();
                s.onNext(new TypeAndNameAndMetric(type, entry.getKey(), entry.getValue()));
            });
            s.onComplete();
        })
                .map(typeAndNameAndMetric -> {
                    Map map = toMap(typeAndNameAndMetric.metric);
                    map.put("name", typeAndNameAndMetric.name);
                    map.put("@timestamp", timestampInUtc);
                    map.put("host", metricsDump.hostName);
                    map.put("hostAddress", metricsDump.hostAddress);
                    map.putAll(additionalMetricAttributes);
                    return new TypeAndMetric(typeAndNameAndMetric.type, map);
                })
                .map(typeAndMetricAsMap -> new IndexRequest()
                                .index(indexName)
                                .type(typeAndMetricAsMap.type)
                                .source(typeAndMetricAsMap.metricAsMap)
                )
                .reduce(client.prepareBulk(),
                        (bulkRequestBuilder, indexRequest) -> {
                            bulkRequestBuilder.add(indexRequest);
                            return bulkRequestBuilder;
                        })
                .map(bulkRequestBuilder -> bulkRequestBuilder.get())
                .subscribe(
                        response -> {
                            if (response.hasFailures()) {
                                logger.warn("Error uploading metrics: " + response.buildFailureMessage());
                            } else {
                                logger.trace("Successfully uploaded {} metric(s)", response.getItems().length);
                            }
                        },
                        exception -> logger.error("Unable to upload metrics to index " + indexName)
                );
    }

    private Map<String, Object> toMap(Metric metric) {
        if (metric instanceof CompoundMetric) return ((CompoundMetric) metric).getValues();
        else return objectMapper.convertValue(metric, Map.class);
    }

    public static class TypeAndMetric {
        public final String type;
        public final Map metricAsMap;

        public TypeAndMetric(String type, Map metricAsMap) {
            this.type = type;
            this.metricAsMap = metricAsMap;
        }
    }

    public static class TypeAndNameAndMetric {
        public final String type;
        public final String name;
        public final Metric metric;

        public TypeAndNameAndMetric(String type, String name, Metric metric) {
            this.type = type;
            this.name = name;
            this.metric = metric;
        }
    }
}
