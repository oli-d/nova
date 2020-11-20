package ch.squaredesk.nova.metrics.elastic;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.http.HttpHost;

public class ElasticConfig {

    public static final String DEFAULT_HOST = "localhost:9200";
    public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_SECONDS = 1;
    public static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 15;
    public static final int DEFAULT_MAX_CONNECTIONS_TOTAL = 45;

    /** Timeout (in seconds) for establishing a new Elasticsearch connection. */
    public final int connectTimeout;
    
    /** Timeout (in seconds) for a new Elasticsearch request. */
    public final int requestTimeout;
    
    /** Timeout (in seconds) for waiting for data (a maximum period of inactivity). */
    public final int socketTimeout;
    
    /** Maximum number of total connections allowed per route. */
    public final int maxConnectionsPerRoute;
    
    /** Maximum number of total connections allowed. */
    public final int maxConnectionsTotal;
    
    /** Name of index used for storing messages. */
    public final String indexName;
    
    /** Path prefix for client calls. */
    public final String pathPrefix;

    /** Connection endpoints . */
    public final List<String> hosts;
    
    /** The basic authentication user for Elasticsearch (use only if the security feature is enabled). */
    public final String user;
    
    /** The basic authentication user password. */
    public final String password;
    
    /** If set to true then indexName-yyyy.MM.dd will be created daily. */
    public final boolean createDailyIndices;

    private ElasticConfig(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.requestTimeout = builder.requestTimeout;
        this.socketTimeout = builder.socketTimeout;
        this.maxConnectionsPerRoute = builder.maxConnectionsPerRoute;
        this.maxConnectionsTotal = builder.maxConnectionsTotal;
        this.indexName = builder.indexName;
        this.pathPrefix = builder.pathPrefix;
        this.hosts = builder.hosts;
        this.user = builder.user;
        this.password = builder.password;
        this.createDailyIndices = builder.createDailyIndices;
    }
    
    public HttpHost[] httpHosts() {
        return hosts.stream().map(HttpHost::create).toArray(HttpHost[]::new);   
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(indexName, hosts, connectTimeout, requestTimeout, socketTimeout, 
                            maxConnectionsPerRoute, maxConnectionsTotal, pathPrefix,
                            user, password, createDailyIndices);
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof ElasticConfig) {
            final ElasticConfig other = (ElasticConfig) object;
            return Objects.equals(indexName, other.indexName) &&
                   Objects.equals(hosts, other.hosts) &&
                   Objects.equals(connectTimeout, other.connectTimeout) &&
                   Objects.equals(requestTimeout, other.requestTimeout) &&
                   Objects.equals(socketTimeout, other.socketTimeout) &&
                   Objects.equals(maxConnectionsPerRoute, other.maxConnectionsPerRoute) &&
                   Objects.equals(maxConnectionsTotal, other.maxConnectionsTotal) &&
                   Objects.equals(pathPrefix, other.pathPrefix) &&
                   Objects.equals(user, other.user) &&
                   Objects.equals(password, other.password) &&
                   Objects.equals(createDailyIndices, other.createDailyIndices);
        } else {
            return false;
        }
    }

    public ElasticConfig replicateFor(String anotherIndexName) {
        return builderFrom(this).indexName(anotherIndexName).build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builderFrom(ElasticConfig otherConfig) {
        return Optional.ofNullable(otherConfig)
                .map(config -> ElasticConfig.builder()
                        .hosts(config.hosts)
                        .user(config.user)
                        .password(config.password)
                        .indexName(config.indexName)
                        .pathPrefix(config.pathPrefix)
                        .createDailyIndices(config.createDailyIndices)
                        .connectTimeout(config.connectTimeout)
                        .requestTimeout(config.requestTimeout)
                        .socketTimeout(config.socketTimeout)
                        .maxConnectionsPerRoute(config.maxConnectionsPerRoute)
                        .maxConnectionsTotal(config.maxConnectionsTotal))
                .orElse(ElasticConfig.builder());
    }
    
    public static final class Builder {
        
        private int connectTimeout;
        private int requestTimeout;
        private int socketTimeout;
        private int maxConnectionsPerRoute;
        private int maxConnectionsTotal;
        private String indexName;
        private String pathPrefix;
        private List<String> hosts;
        private String user;
        private String password;
        private boolean createDailyIndices;

        private Builder() { }

        public ElasticConfig build() {
            ensureDefaultValuesWhereNeeded();
            return new ElasticConfig(this);
        }
        
        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder requestTimeout(int requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder socketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder maxConnectionsPerRoute(int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            return this;
        }

        public Builder maxConnectionsTotal(int maxConnectionsTotal) {
            this.maxConnectionsTotal = maxConnectionsTotal;
            return this;
        }

        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder pathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
            return this;
        }

        public Builder hosts(String elasticHost) {
            return hosts(elasticHost != null ? null : Arrays.asList(elasticHost));
        }
        
        public Builder hosts(List<String> hosts) {
            this.hosts = hosts;
            return this;
        }
        
        public Builder user(String user) {
            this.user = user;
            return this;
        }
        
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        
        public Builder createDailyIndices(boolean createDailyIndices) {
            this.createDailyIndices = createDailyIndices;
            return this;
        }
        
        private void ensureDefaultValuesWhereNeeded() {
            if (hosts == null || hosts.isEmpty()) {
                hosts(DEFAULT_HOST);
            }
            
            connectTimeout = ensureDefaultIfZero(connectTimeout, DEFAULT_CONNECT_TIMEOUT_SECONDS);
            requestTimeout = ensureDefaultIfZero(requestTimeout, DEFAULT_CONNECTION_REQUEST_TIMEOUT_SECONDS);
            socketTimeout = ensureDefaultIfZero(socketTimeout, DEFAULT_SOCKET_TIMEOUT_SECONDS);

            maxConnectionsPerRoute = ensureDefaultIfZeroOrNegative(maxConnectionsPerRoute, DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
            maxConnectionsTotal = ensureDefaultIfZeroOrNegative(maxConnectionsTotal, DEFAULT_MAX_CONNECTIONS_TOTAL);
        }

        private int ensureDefaultIfZero(int actualValue, int defaultIfZero) {
            return actualValue == 0 ? defaultIfZero : actualValue;
        }
        
        private int ensureDefaultIfZeroOrNegative(int actualValue, int defaultIfZeroOrNegative) {
            return actualValue <= 0 ? defaultIfZeroOrNegative : actualValue;
        }
    }
}
