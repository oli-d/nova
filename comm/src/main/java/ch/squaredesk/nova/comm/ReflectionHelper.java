/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class ReflectionHelper {
    /**
     * Since Lambdas do erase type information, the method only works if the passed instance to examine is
     * NOT a lambda!!!
     * <p>
     * See: https://stackoverflow.com/questions/21887358/reflection-type-inference-on-java-8-lambdas
     */
    public static Class<?> getConcreteTypeOfGenericInterfaceImplementation(
            Object instanceToExamine,
            Class<?> implementedInterfaceClass,
            int parameterIndex) {

        Type genericInterfaceType = Arrays.stream(instanceToExamine.getClass().getGenericInterfaces())
                .filter(t -> t.getTypeName().contains(implementedInterfaceClass.getName()))
                .findFirst()
                .get();

        if (genericInterfaceType instanceof ParameterizedType) {
            Type[] actualTypes = ((ParameterizedType) genericInterfaceType).getActualTypeArguments();
            if (actualTypes.length >= parameterIndex) {
                return (Class) actualTypes[parameterIndex];
            }
        }

        return null;
    }

    public static Class<?> getConcreteTypeOfGenericClassExtension(
            Class<?> classToExamine,
            int parameterIndex) {

        Type genericSuperclassType = classToExamine.getGenericSuperclass();
        if (genericSuperclassType instanceof ParameterizedType) {
            Type[] actualTypes = ((ParameterizedType) genericSuperclassType).getActualTypeArguments();
            if (actualTypes.length >= parameterIndex) {
                return (Class) actualTypes[parameterIndex];
            }
        }

        return null;
    }

    public static Object instanceFromClassName(String className) {
        Class classObject;
        try {
            classObject = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load class " + className);
        }
        try {
            return classObject.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to instantiate class " + className);
        }
    }

}
