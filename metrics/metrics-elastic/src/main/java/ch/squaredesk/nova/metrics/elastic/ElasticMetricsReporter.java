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

import ch.squaredesk.nova.metrics.MetricsConverter;
import ch.squaredesk.nova.metrics.MetricsDump;
import ch.squaredesk.nova.metrics.SerializableMetricsDump;
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
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ElasticMetricsReporter implements Consumer<MetricsDump> {
    private static final Logger logger = LoggerFactory.getLogger(ElasticMetricsReporter.class);

    private final Consumer<Throwable> defaultExceptionHandler;
    private final String elasticServer;
    private final int elasticPort;
    private final String indexName;
    private final Map<String, Object> additionalMetricAttributes;
    private final ZoneId zoneForTimestamps = ZoneId.of("UTC");
    private RestClient restClient;
    private RestHighLevelClient client;

    public ElasticMetricsReporter(String elasticServer, int elasticPort, String indexName) {
        this(elasticServer, elasticPort, indexName, Collections.emptyMap());
    }

    public ElasticMetricsReporter(String elasticServer, int elasticPort, String indexName, Map<String, Object> additionalMetricAttributes) {
        this.elasticServer = elasticServer;
        this.elasticPort = elasticPort;
        this.indexName = indexName;
        this.additionalMetricAttributes = additionalMetricAttributes == null ? Collections.emptyMap() : additionalMetricAttributes;
        defaultExceptionHandler = exception -> logger.error("Unable to upload metrics to index " + indexName, exception);
    }


    @Override
    public void accept(MetricsDump metricsDump) {
        accept(metricsDump, defaultExceptionHandler);
    }

    public void accept(MetricsDump metricsDump, Consumer<Throwable> exceptionHandler) {
        if (client == null) {
            throw new IllegalStateException("not started yet");
        }
        fireRequest(requestFor(metricsDump), exceptionHandler);
    }


    public void accept(SerializableMetricsDump metricsDump){
        accept(metricsDump, defaultExceptionHandler);
    }

    public void accept(SerializableMetricsDump metricsDump, Consumer<Throwable> exceptionHandler) {
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

    private long timestampInUtc(long timestampInMillis) {
        return Instant.ofEpochMilli(timestampInMillis).atZone(zoneForTimestamps).toInstant().toEpochMilli();
    }

    private Single<BulkRequest> requestFor (Observable<Map<String, Object>> metricsAsMaps) {
        return metricsAsMaps
                .map(map -> new IndexRequest()
                                .index(indexName)
                                .type("doc")
                                .source(map)
                )
                .reduce(Requests.bulkRequest(),
                        (bulkRequestBuilder, indexRequest) -> {
                            bulkRequestBuilder.add(indexRequest);
                            return bulkRequestBuilder;
                        })
                ;

    }

    Single<BulkRequest> requestFor(Map<String, Object> metricsDump) {
        Long timestamp = (Long) metricsDump.remove("timestamp");
        long timestampInUtc = timestamp == null ? timestampInUtc(System.currentTimeMillis()) : timestampInUtc(timestamp);
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
                    metricAsMap.put("@timestamp", timestampInUtc);
                    metricAsMap.put("host", hostName);
                    metricAsMap.put("hostAddress", hostAddress);
                    metricAsMap.putAll(additionalMetricAttributes);
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

    Single<BulkRequest> requestFor(MetricsDump metricsDump) {
        Map<String, Object> additionalAttributes = new HashMap<>();
        long timestampInUtc = timestampInUtc(metricsDump.timestamp);
        additionalAttributes.put("@timestamp", timestampInUtc);
        additionalAttributes.put("host", metricsDump.hostName);
        additionalAttributes.put("hostAddress", metricsDump.hostAddress);
        additionalAttributes.putAll(additionalMetricAttributes);

        Map<String, Map<String, Object>> convertedDump = MetricsConverter.convert(metricsDump.metrics, additionalAttributes);
        Observable<Map<String, Object>> enrichedMetrics = Observable.fromIterable(convertedDump.entrySet())
                .map(entry -> {
                    Map<String, Object> map = entry.getValue();
                    map.put("name", entry.getKey());
                    return map;
                });
        return requestFor(enrichedMetrics);
    }

    Single<BulkRequest> requestFor(SerializableMetricsDump metricsDump) {
        Map<String, Object> additionalAttributes = new HashMap<>();
        long timestampInUtc = timestampInUtc(metricsDump.timestamp);
        additionalAttributes.put("@timestamp", timestampInUtc);
        additionalAttributes.put("host", metricsDump.hostName);
        additionalAttributes.put("hostAddress", metricsDump.hostAddress);
        additionalAttributes.putAll(additionalMetricAttributes);

        Observable<Map<String, Object>> enrichedMetrics = Observable.fromIterable(metricsDump.metrics.entrySet())
                .map(entry -> {
                    Map<String, Object> map = entry.getValue();
                    map.put("name", entry.getKey());
                    map.putAll(additionalAttributes);
                    return map;
                });
        return requestFor(enrichedMetrics);
    }

}
