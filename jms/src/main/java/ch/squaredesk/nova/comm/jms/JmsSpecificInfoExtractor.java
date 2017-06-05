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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

class JmsSpecificInfoExtractor {

    static JmsSpecificInfo extractFrom (Message message) throws JMSException {
        Destination replyTo = message.getJMSReplyTo();
        String correlationId = message.getJMSCorrelationID();
        int deliveryMode = message.getJMSDeliveryMode();
        int priority = message.getJMSPriority();
        long timeToLive = message.getJMSExpiration();
        Map<String, Object> customHeaders = null;

        Enumeration<String> enumeration = message.getPropertyNames();
        if (enumeration.hasMoreElements()) {
            customHeaders = new HashMap<>(3, 1.0f);
            while (enumeration.hasMoreElements()) {
                String key = enumeration.nextElement();
                customHeaders.put(key, String.valueOf(message.getObjectProperty(key)));
            }
        }

        return new JmsSpecificInfo(
                correlationId, replyTo, customHeaders, deliveryMode, priority, timeToLive);
    }

}
