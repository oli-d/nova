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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

public class TestConnectionFactory implements ConnectionFactory{
    private final Connection connection;


    public TestConnectionFactory(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection createConnection() throws JMSException {
        return connection;
    }

    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        return connection;
    }
}
