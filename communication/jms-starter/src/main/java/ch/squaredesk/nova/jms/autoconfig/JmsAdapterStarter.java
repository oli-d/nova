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
package ch.squaredesk.nova.jms.autoconfig;

import ch.squaredesk.nova.comm.jms.JmsAdapter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class JmsAdapterStarter implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {
    private final JmsAdapter jmsAdapter;

    public JmsAdapterStarter(JmsAdapter jmsAdapter) {
        this.jmsAdapter = jmsAdapter;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws Exception {
        jmsAdapter.start();
    }

    @Override
    public void destroy() throws Exception {
        if (jmsAdapter != null) {
            try {
                jmsAdapter.shutdown();
            } catch (Exception e) {
                // noop; shutdown anyway...
            }
        }
    }
}
