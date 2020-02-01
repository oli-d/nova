/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.http;

import com.ning.http.client.AsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Map;

public class HttpClientBeanNotifier implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientBeanNotifier.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Object httpClient = event.getApplicationContext().getBean(BeanIdentifiers.SERVER);
        if (httpClient instanceof AsyncHttpClient) { // can also be a NullInstance
            notifyHttpClientAvailableInContext((AsyncHttpClient)httpClient, event.getApplicationContext());
        }
    }

    public static void notifyHttpClientAvailableInContext(AsyncHttpClient httpClient, ApplicationContext context) {
        Map<String, HttpClientInstanceListener> beans = context.getBeansOfType(HttpClientInstanceListener.class);
        if (beans != null && ! beans.isEmpty()) {
            beans.entrySet().forEach(entry -> {
                try {
                    entry.getValue().httpClientInstanceCreated(httpClient);
                } catch (Exception e) {
                    logger.error("An error occurred trying to notify bean {} about HttpClient instance", entry.getKey(), e);
                }
            });
        }
    }


}
