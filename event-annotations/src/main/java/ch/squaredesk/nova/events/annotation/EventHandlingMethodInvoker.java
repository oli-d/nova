/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events.annotation;

import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

public class EventHandlingMethodInvoker implements Consumer<Object[]> {
    private final static Logger LOGGER = LoggerFactory.getLogger(EventHandlingMethodInvoker.class);
    private final Object objectToInvokeMethodOn;
    private final Method methodToInvoke;
    private final EventContext eventContext;
    private final boolean injectEventContext;

    public EventHandlingMethodInvoker(Object objectToInvokeMethodOn, Method methodToInvoke, EventContext eventContext) {
        this.objectToInvokeMethodOn = objectToInvokeMethodOn;
        this.methodToInvoke = methodToInvoke;
        this.eventContext = eventContext;
        Class classOfLastParameter = methodToInvoke.getParameterTypes()[methodToInvoke.getParameterTypes().length-1];
        this.injectEventContext = (classOfLastParameter == EventContext.class);
    }

    @Override
    public void accept(Object... data) {
        Object[] parameterArray = createParameterArrayFromEventEmitterData(methodToInvoke.getParameterCount(),data);
        try {
            if (parameterArray==null) {
                methodToInvoke.invoke(objectToInvokeMethodOn);
            } else {
                methodToInvoke.invoke(objectToInvokeMethodOn, parameterArray);
            }
        } catch (Throwable t) {
            LOGGER.error("Unable to invoke event Handler");
            LOGGER.error("\tParameters: ");
            if (data!=null) {
                Arrays.stream(data).forEach(param -> LOGGER.error("\t\t" + param));
                LOGGER.error("\t\tnull");
            } else {
                LOGGER.error("\t\tnull");
            }
            LOGGER.error("\tException: ",t);
        }
    }

    private Object[] createParameterArrayFromEventEmitterData(int numParametersForMethodCall, Object...dataArray) {
        if (numParametersForMethodCall == 0) {
            return null;
        }

        Object[] retVal = new Object[numParametersForMethodCall];
        if (dataArray!=null) {
            int numElementsToCopyFromDataArray = Math.min(retVal.length,dataArray.length);
            System.arraycopy(dataArray, 0, retVal, 0, numElementsToCopyFromDataArray);
        }

        if (injectEventContext) {
            retVal[retVal.length-1] = eventContext;
        }
        return retVal;
    }
}
