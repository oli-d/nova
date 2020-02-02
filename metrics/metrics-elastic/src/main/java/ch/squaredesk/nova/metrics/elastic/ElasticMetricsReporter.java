/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
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
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class ElasticMetricsReporter implements Consumer<MetricsDump> {
    private static final Logger logger = LoggerFactory.getLogger(ElasticMetricsReporter.class);

    private final Consumer<Throwable> defaultExceptionHandler;
    private final HttpHost[] elasticServers;
    private final String indexName;
    private final ZoneId zoneForTimestamps = ZoneId.of("UTC");
    private RestClient restClient;
    private RestHighLevelClient client;

    public ElasticMetricsReporter(String indexName, String ... elasticServers) {
        this(indexName,
                Arrays.stream(elasticServers)
                        .map(HttpHost::new)
                        .toArray(HttpHost[]::new));
    }

    public ElasticMetricsReporter(String indexName, HttpHost ... elasticServers) {
        this.elasticServers = elasticServers;
        this.indexName = indexName;
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
        logger.info("Connecting to Elasticsearch @ {}", Arrays.toString(elasticServers));
        try {
            HttpHost[] httpHosts = Arrays.stream(elasticServers).map(HttpHost::new).toArray(HttpHost[]::new);
            RestClientBuilder restClientBuilder = RestClient.builder(httpHosts);
            restClient = restClientBuilder.build();
            client = new RestHighLevelClient(restClientBuilder);
        } catch (Exception e) {
            logger.error("Unable to connect to Elastic @ {}", Arrays.toString(elasticServers), e);
        }
        logger.info("\tsuccessfully established connection to Elastic @ {} :-)", Arrays.toString(elasticServers));
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
                    BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                    if (response.hasFailures()) {
                        logger.warn("Error uploading metrics: " + response.buildFailureMessage());
                    } else {
                        logger.trace("Successfully uploaded {} metric(s)", response.getItems().length);
                    }
                },
                exceptionHandler
        );
    }

    private String timestampInUtc(long timestampInMillis) {
        return Instant.ofEpochMilli(timestampInMillis)
                .atZone(zoneForTimestamps)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

    private Single<BulkRequest> requestFor (Observable<Map<String, Object>> metricsAsMaps) {
        return metricsAsMaps
                .map(map -> new IndexRequest()
                                .index(indexName)
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
        String timestampInUtc = timestamp == null ? timestampInUtc(System.currentTimeMillis()) : timestampInUtc(timestamp);
        return Observable.fromIterable(metricsDump.entrySet())
                .filter(entry -> entry.getValue() instanceof Map) // just to protect us, since at this point anyway
                // only the Metrics should remain in the map
                // and they are all represented as Map<String, Object>
                .map(entry -> {
                    Map<String, Object> retVal = (Map) entry.getValue();
                    retVal.put("name", entry.getKey());
                    retVal.put("@timestamp", timestampInUtc);
                    return retVal;
                })
                .map(metricAsMap -> new IndexRequest()
                            .index(indexName)
                            .type("doc")
                            .source(metricAsMap)
                )
                .reduce(Requests.bulkRequest(),
                        (bulkRequestBuilder, indexRequest) -> {
                            bulkRequestBuilder.add(indexRequest);
                            return bulkRequestBuilder;
                        });
    }

    Single<BulkRequest> requestFor(MetricsDump metricsDump) {
        String timestampInUtc = timestampInUtc(metricsDump.timestamp);

        Map<String, Map<String, Object>> convertedDump = MetricsConverter.convert(metricsDump);
        Observable<Map<String, Object>> enrichedMetrics = Observable.fromIterable(convertedDump.entrySet())
                .map(entry -> {
                    Map<String, Object> map = entry.getValue();
                    map.put("name", entry.getKey());
                    map.put("@timestamp", timestampInUtc);
                    return map;
                });
        return requestFor(enrichedMetrics);
    }

    Single<BulkRequest> requestFor(SerializableMetricsDump metricsDump) {
        String timestampInUtc = timestampInUtc(metricsDump.getTimestamp());

        Observable<Map<String, Object>> enrichedMetrics = Observable.fromIterable(metricsDump.getMetrics().entrySet())
                .map(entry -> {
                    Map<String, Object> map = entry.getValue();
                    map.put("name", entry.getKey());
                    map.put("@timestamp", timestampInUtc);
                    return map;
                });
        return requestFor(enrichedMetrics);
    }

}
