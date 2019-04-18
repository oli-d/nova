/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.jms.spring;

import ch.squaredesk.nova.comm.jms.JmsAdapter;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.PreDestroy;
import java.io.IOException;

public class JmsAdapterStarter implements ApplicationListener<ContextRefreshedEvent> {
    private final JmsAdapter jmsAdapter;
    private final boolean autoStartAdapterWhenApplicationContextRefreshed;

    public JmsAdapterStarter(JmsAdapter jmsAdapter, boolean autoStartAdapterWhenApplicationContextRefreshed) {
        this.jmsAdapter = jmsAdapter;
        this.autoStartAdapterWhenApplicationContextRefreshed = autoStartAdapterWhenApplicationContextRefreshed;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (jmsAdapter!= null && autoStartAdapterWhenApplicationContextRefreshed) {
            try {
                start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void start() throws Exception {
        jmsAdapter.start();
    }

    @PreDestroy
    public void shutdown() {
        if (jmsAdapter != null) {
            try {
                jmsAdapter.shutdown();
            } catch (Exception e) {
                // noop; shutdown anyway...
            }
        }
    }


}
