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
import java.util.*;

class AdminCommandMethodInvoker implements Consumer<Map<String, String>> {
    private final static Logger LOGGER = LoggerFactory.getLogger(AdminCommandMethodInvoker.class);
    private final AdminCommandConfig adminCommandConfig;

    AdminCommandMethodInvoker(AdminCommandConfig adminCommandConfig) {
        this.adminCommandConfig = adminCommandConfig;
    }

    @Override
    public void accept(Map<String, String> parameters) {
        Set<String> paramNameSet = new HashSet<>(Arrays.asList(adminCommandConfig.parameterNames));
        if (!parameters.keySet().equals(paramNameSet)) {
            throw new IllegalArgumentException("wrong parameter set");
        }

        Object[] parameterArray = parameterArrayFor(parameters);
        try {
            if (parameterArray==null) {
                adminCommandConfig.methodToInvoke.invoke(adminCommandConfig.objectToInvokeMethodOn);
            } else {
                adminCommandConfig.methodToInvoke.invoke(adminCommandConfig.objectToInvokeMethodOn, parameterArray);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object[] parameterArrayFor(Map<String, String> params) {
        if (adminCommandConfig.parameterNames.length == 0) {
            return null;
        }

        Object[] retVal = new Object[adminCommandConfig.parameterNames.length];
        for (int i=0; i<adminCommandConfig.parameterNames.length; i++) {
            retVal[i] = params.get(adminCommandConfig.parameterNames[i]);
        }

        return retVal;
    }
}
