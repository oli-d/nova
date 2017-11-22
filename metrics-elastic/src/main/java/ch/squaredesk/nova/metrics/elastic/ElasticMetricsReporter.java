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
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private final String indexName;
    private final Map<String, Object> additionalMetricAttributes;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ZoneId zoneForTimestamps = ZoneId.of("UTC");
    private RestClient restClient;
    private RestHighLevelClient client;

    public ElasticMetricsReporter(String elasticServer, int elasticPort, String indexName) {
        this(elasticServer, elasticPort, indexName, Collections.EMPTY_MAP);
    }

    public ElasticMetricsReporter(String elasticServer, int elasticPort, String indexName, Map<String, Object> additionalMetricAttributes) {
        this.elasticServer = elasticServer;
        this.elasticPort = elasticPort;
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
        fireRequest(requestFor(metricsDump), exceptionHandler);
    }

    /**
     * Sends a metrics dump, created with MetricsDumpToMapConverter, to Elastic.
     */
    public void accept(Map<String, Object> metricsDump) throws Exception {
        accept(metricsDump, defaultExceptionHandler);
    }

    /**
     * Sends a metrics dump, created with MetricsDumpToMapConverter, to Elastic.
     */
    public void accept(Map<String, Object> metricsDump, Consumer<Throwable> exceptionHandler) throws Exception {
        if (client == null) {
            throw new IllegalStateException("not started yet");
        }
        fireRequest(requestFor(metricsDump), exceptionHandler);
    }

    public void startup() {
        logger.info("Connecting to Elasticsearch @ " + elasticServer + ":" + elasticPort);
        try {
            RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(elasticServer, elasticPort, "http"));
            restClient = restClientBuilder.build();
            client = new RestHighLevelClient(restClientBuilder);
        } catch (Exception e) {
            logger.error("Unable to connect to Elastic @ " + elasticServer + ":" + elasticPort, e);
        }
        logger.info("\tsuccessfully established connection to Elastic @ " + elasticServer + ":" + elasticPort + " :-)");
    }

    public void shutdown() {
        if (client != null) {
            logger.info("Shutting down connection to Elasticsearch");
            try {
                restClient.close();
                logger.info("\tsuccessfully shutdown connection to Elasticsearch :-)");
            } catch (IOException e) {
                logger.info("Error, trying to close connection to ElasticSearch", e);
            }
        }
    }

    void fireRequest(Single<BulkRequest> bulkRequestSingle,
                            Consumer<Throwable> exceptionHandler) {
        Objects.requireNonNull(exceptionHandler, "exceptionHandler must not be null");

        bulkRequestSingle.subscribe(
                bulkRequest -> {
                    BulkResponse response = client.bulk(bulkRequest);
                    if (response.hasFailures()) {
                        logger.warn("Error uploading metrics: " + response.buildFailureMessage());
                    } else {
                        logger.trace("Successfully uploaded {} metric(s)", response.getItems().length);
                    }
                },
                exceptionHandler
        );
    }

    Single<BulkRequest> requestFor(Map<String, Object> metricsDump) throws Exception {
        Long timestamp = (Long) metricsDump.remove("timestamp");
        LocalDateTime timestampInUtc = timestamp == null ? null : timestampInUtc(timestamp);
        String hostName = (String) metricsDump.remove("hostName");
        String hostAddress = (String) metricsDump.remove("hostAddress");
        return Observable.fromIterable(metricsDump.entrySet())
                .filter(entry -> entry.getValue() instanceof Map) // just to protect us, since at this point anyway
                // only the Metrics should remain in the map
                // and they are all represented as Map<String, Object>
                .map(entry -> {
                    Map<String, Object> retVal = (Map) entry.getValue();
                    retVal.put("name", entry.getKey());
                    return retVal;
                })
                .map(metricAsMap -> {
                    Objects.requireNonNull(metricAsMap.get("type"), "metricMap must contain type entry");
                    metricAsMap.put("@timestamp", timestampInUtc);
                    metricAsMap.put("host", hostName);
                    metricAsMap.put("hostAddress", hostAddress);
                    return new IndexRequest()
                            .index(indexName)
                            .type("doc")
                            .source(metricAsMap);
                })
                .reduce(Requests.bulkRequest(),
                        (bulkRequestBuilder, indexRequest) -> {
                            bulkRequestBuilder.add(indexRequest);
                            return bulkRequestBuilder;
                        });
    }

    private LocalDateTime timestampInUtc(long timestampInMillis) {
        return Instant.ofEpochMilli(timestampInMillis).atZone(zoneForTimestamps).toLocalDateTime();
    }

    Single<BulkRequest> requestFor(MetricsDump metricsDump) throws Exception {
        LocalDateTime timestampInUtc = timestampInUtc(metricsDump.timestamp);

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
                .reduce(Requests.bulkRequest(),
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
