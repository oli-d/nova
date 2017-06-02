/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.service.admin;

import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

public class AdminCommandInvoker implements Consumer<Object[]> {
    private final static Logger LOGGER = LoggerFactory.getLogger(AdminCommandInvoker.class);
    private final Object objectToInvokeMethodOn;
    private final Method methodToInvoke;

    public AdminCommandInvoker(Object objectToInvokeMethodOn, Method methodToInvoke) {
        this.objectToInvokeMethodOn = objectToInvokeMethodOn;
        this.methodToInvoke = methodToInvoke;
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

        return retVal;
    }
}
