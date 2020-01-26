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

package ch.squaredesk.nova.comm.rest;

import ch.squaredesk.nova.comm.http.AsyncHttpClientFactory;
import ch.squaredesk.nova.comm.http.BeanIdentifiers;
import ch.squaredesk.nova.comm.http.HttpClientBeanNotifier;
import ch.squaredesk.nova.comm.http.HttpClientSettings;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.filter.RequestFilter;
import com.ning.http.client.filter.ResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Map;

public class RestClientStarter implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(RestClientStarter.class);

    private AsyncHttpClient httpClient;
    private final HttpClientSettings httpClientSettings;

    public RestClientStarter(HttpClientSettings httpClientSettings) {
        this.httpClientSettings = httpClientSettings;
    }


    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (httpClient == null) {
            AsyncHttpClientConfig.Builder builder = AsyncHttpClientFactory.builderFor(httpClientSettings);

            // register request filters and interceptors
            Map<String, RequestFilter> requestFilters =
                    contextRefreshedEvent.getApplicationContext().getBeansOfType(RequestFilter.class);
            requestFilters.values().forEach(filter -> {
                logger.debug("Registering client request filter of type {}", filter.getClass().getName());
                builder.addRequestFilter(filter);
            });
            Map<String, ResponseFilter> responseFilters =
                    contextRefreshedEvent.getApplicationContext().getBeansOfType(ResponseFilter.class);
            responseFilters.values().forEach(filter -> {
                logger.debug("Registering client response filter of type {}", filter.getClass().getName());
                builder.addResponseFilter(filter);
            });

            httpClient = AsyncHttpClientFactory.clientFor(builder.build());

            // register client in ApplicationContext
            GenericApplicationContext genericContext = (GenericApplicationContext) contextRefreshedEvent.getApplicationContext();
            genericContext.registerBean(BeanIdentifiers.CLIENT, AsyncHttpClient.class, () -> httpClient);

            // notify everyone interested
            HttpClientBeanNotifier.notifyHttpClientAvailableInContext(httpClient, contextRefreshedEvent.getApplicationContext());
        }
    }

    @Override
    public void destroy() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
