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

import ch.squaredesk.nova.comm.rest.HttpSpecificInfo;
import ch.squaredesk.nova.comm.rpc.RpcInvocation;
import ch.squaredesk.nova.service.admin.messages.AdminMessage;
import ch.squaredesk.nova.service.admin.messages.Error;
import ch.squaredesk.nova.service.admin.messages.Reply;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class AdminCommandMethodInvoker implements Consumer<RpcInvocation<AdminMessage, AdminMessage, HttpSpecificInfo>> {
    private final static Logger logger = LoggerFactory.getLogger(AdminCommandMethodInvoker.class);
    private final AdminCommandConfig adminCommandConfig;

    AdminCommandMethodInvoker(AdminCommandConfig adminCommandConfig) {
        this.adminCommandConfig = adminCommandConfig;
    }

    @Override
    public void accept(RpcInvocation<AdminMessage, AdminMessage, HttpSpecificInfo> invocation) {
        AdminMessage reply;

        Set<String> paramNameSet = new HashSet<>(Arrays.asList(adminCommandConfig.parameterNames));
        if (!invocation.transportSpecificInfo.parameters.keySet().containsAll(paramNameSet)) {
            reply = new Error("wrong parameter set");
        }

        try {
            Object[] parameterArray = parameterArrayFor(invocation.transportSpecificInfo.parameters);
            String result;
            if (parameterArray==null) {
                result = (String)adminCommandConfig.methodToInvoke.invoke(adminCommandConfig.objectToInvokeMethodOn);
            } else {
                result = (String)adminCommandConfig.methodToInvoke.invoke(adminCommandConfig.objectToInvokeMethodOn, parameterArray);
            }
            reply = new Reply(result);
        } catch (Exception e) {
            logger.error("An error occurred, trying to invoke adminCommand " + adminCommandConfig.methodToInvoke.getName(),e);
            reply = new Error("Failed to invoke admin command. " + e.getLocalizedMessage());
        }

        invocation.complete(reply);
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
