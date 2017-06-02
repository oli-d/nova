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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

class BeanExaminer {
    void examine (Object bean, Consumer<AdminCommandConfig> configConsumer) {
        Objects.requireNonNull(bean, "bean to examine must not be null");
        Objects.requireNonNull(configConsumer, "configConsumer must not be null");

        Method[] methods = bean.getClass().getMethods();
        Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(OnAdminCommand.class))
                .forEach(m -> {
                    OnAdminCommand annotation = m.getAnnotation(OnAdminCommand.class);
                    String[] parameterNames = annotation.value();

                    assertProperMethodReturnValue(m);
                    assertProperParameterDeclaration(parameterNames, m);
                    configConsumer.accept(new AdminCommandConfig(bean, m, parameterNames));
                });
    }

    private void assertProperMethodReturnValue (Method methodToExamine) {
        if (methodToExamine.getReturnType() != String.class) {
            throw new IllegalArgumentException("Method " +
                    methodToExamine.getName() + " must return String to be considered a valid admin command handler.");
        }
    }

    private void assertProperParameterDeclaration (String[] paramNames, Method methodToExamine) {
        if  (methodToExamine.getParameterCount() != paramNames.length){
            throw new IllegalArgumentException("Invalid parameter definition. " +
                    paramNames.length + " parameter(s) defined, " +
                    methodToExamine.getParameterCount() + " expected.");
        }
    }
}
