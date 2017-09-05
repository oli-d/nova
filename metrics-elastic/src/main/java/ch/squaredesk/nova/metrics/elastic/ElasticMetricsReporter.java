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
import ch.squaredesk.nova.tuples.Pair;
import ch.squaredesk.nova.tuples.Tuple3;
import com.codahale.metrics.Metric;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
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
import java.util.Objects;

public class ElasticMetricsReporter implements Consumer<MetricsDump> {
    private static final Logger logger = LoggerFactory.getLogger(ElasticMetricsReporter.class);

    private final Consumer<Throwable> defaultExceptionHandler;
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
        defaultExceptionHandler = exception -> logger.error("Unable to upload metrics to index " + indexName, exception);
    }


    @Override
    public void accept(MetricsDump metricsDump) throws Exception {
        accept(metricsDump, defaultExceptionHandler);
    }

    public void accept(MetricsDump metricsDump, Consumer<Throwable> exceptionHandler) throws Exception {
        if (client == null) {
            throw new IllegalStateException("not started yet");
        }
        fireRequest(requestBuilderFor(metricsDump), exceptionHandler);
    }

    public void accept(Map<String, Map<String, Object>> metricsDump) throws Exception {
        accept(metricsDump, defaultExceptionHandler);
    }

    /**
     * Sends a metrics dump, represented as a Map to Elastic.
     *
     * Basically, this method dumps generic Maps to Elasticsearch with the following restriction
     * - each Metric is represented as a Map added to the root Map with metric name = key
     * - each (sub) map, representing a metric must contain a "_type" key and value
     * @param metricsDump
     * @param exceptionHandler
     * @throws Exception
     */
    public void accept(Map<String, Map<String, Object>> metricsDump, Consumer<Throwable> exceptionHandler) throws Exception {
        if (client == null) {
            throw new IllegalStateException("not started yet");
        }
        fireRequest(requestBuilderFor(metricsDump), exceptionHandler);
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

    public void fireRequest (Single<BulkRequestBuilder> bulkRequestBuilderSingle,
                             Consumer<Throwable> exceptionHandler) {
        Objects.requireNonNull(exceptionHandler, "exceptionHandler must not be null");

        bulkRequestBuilderSingle
                .map(bulkRequestBuilder -> bulkRequestBuilder.get())
                .subscribe(
                        response -> {
                            if (response.hasFailures()) {
                                logger.warn("Error uploading metrics: " + response.buildFailureMessage());
                            } else {
                                logger.trace("Successfully uploaded {} metric(s)", response.getItems().length);
                            }
                        },
                        exceptionHandler
                );
    }

    private Single<BulkRequestBuilder> requestBuilderFor (Map<String, Map<String, Object>> metricsDump) throws Exception {
        return Observable.fromIterable(metricsDump.entrySet())
                .map(entry -> {
                    Map<String, Object> retVal = entry.getValue();
                    retVal.put("name", entry.getKey());
                    return retVal;
                })
                .map(metricAsMap -> {
                    String type = (String) metricAsMap.remove("_type");
                    Objects.requireNonNull(type, "metricMap must contain type entry");
                    return new IndexRequest()
                            .index(indexName)
                            .type(type)
                            .source(metricAsMap);
                })
                .reduce(client.prepareBulk(),
                        (bulkRequestBuilder, indexRequest) -> {
                            bulkRequestBuilder.add(indexRequest);
                            return bulkRequestBuilder;
                });
    }

    private Single<BulkRequestBuilder> requestBuilderFor (MetricsDump metricsDump) throws Exception {
        LocalDateTime timestampInUtc = Instant.ofEpochMilli(metricsDump.timestamp).atZone(zoneForTimestamps).toLocalDateTime();

        return Observable.fromIterable(metricsDump.metrics.entrySet())
                .map(entry -> new Tuple3<>(entry.getValue().getClass().getSimpleName(), entry.getKey(), entry.getValue()))
                .map(tupleTypeAndNameAndMetric -> {
                    Map<String, Object> map = toMap(tupleTypeAndNameAndMetric._3);
                    map.put("name", tupleTypeAndNameAndMetric._2);
                    map.put("@timestamp", timestampInUtc);
                    map.put("host", metricsDump.hostName);
                    map.put("hostAddress", metricsDump.hostAddress);
                    map.putAll(additionalMetricAttributes);
                    return new Pair<>(tupleTypeAndNameAndMetric._1, map);
                })
                .map(typeAndMetricAsMap -> new IndexRequest()
                                .index(indexName)
                                .type(typeAndMetricAsMap._1)
                                .source(typeAndMetricAsMap._2)
                )
                .reduce(client.prepareBulk(),
                        (bulkRequestBuilder, indexRequest) -> {
                            bulkRequestBuilder.add(indexRequest);
                            return bulkRequestBuilder;
                })
                ;
    }

    private Map<String, Object> toMap(Metric metric) {
        if (metric instanceof CompoundMetric) return ((CompoundMetric) metric).getValues();
        else return objectMapper.convertValue(metric, Map.class);
    }

}
