/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.jms;


import javax.jms.*;
import java.util.function.Supplier;

public class TestConnection implements Connection {
    private final java.util.function.Supplier<Session> sessionSupplier;

    public TestConnection(Supplier<Session> sessionSupplier) {
        this.sessionSupplier = sessionSupplier;
    }

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        if (sessionSupplier!=null) return sessionSupplier.get();
        else return null;
    }

    @Override
    public String getClientID() throws JMSException {
        return null;
    }

    @Override
    public void setClientID(String clientID) throws JMSException {

    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return null;
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        return null;
    }

    @Override
    public void setExceptionListener(ExceptionListener listener) throws JMSException {

    }

    @Override
    public void start() throws JMSException {

    }

    @Override
    public void stop() throws JMSException {

    }

    @Override
    public void close() throws JMSException {

    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        return null;
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        return null;
    }
}
