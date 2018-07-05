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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static ch.squaredesk.nova.comm.ReflectionHelper.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionHelperTest {
    @Test
    void newInstance() {
        assertTrue(instanceFromClassName("java.util.ArrayList") instanceof ArrayList);
        assertThrows(Throwable.class, () -> instanceFromClassName("java.lang.Double"));
        assertThrows(Throwable.class, () -> instanceFromClassName("java.lang.xxx"));
    }

    @Test
    void genericInterfaceImplementation() {
        assertThat(
                getConcreteTypeOfGenericInterfaceImplementation(new Interface<Double>() {
                    @Override
                    public void someMethod(Double t) {
                    }
                }, Interface.class, 0),
                is(Double.class));
        assertThat(
                getConcreteTypeOfGenericInterfaceImplementation(new InterfaceImpl(), Interface.class, 0),
                is(Double.class));
        // lambdas do not work
        Interface<String> implementingLambda = s -> {};
        assertNull(
                getConcreteTypeOfGenericInterfaceImplementation(implementingLambda, Interface.class, 0));
    }

    @Test
    void genericClassExtension() {
        assertThat(
                getConcreteTypeOfGenericClassExtension(AbstractClassImpl.class, 0),
                is(Double.class));
        assertThat(
                getConcreteTypeOfGenericClassExtension(new AbstractClassImpl().getClass(), 0),
                is(Double.class));
    }

    public interface Interface<Type> {
        void someMethod (Type t);
    }

    public static class InterfaceImpl implements Interface<Double> {
        @Override
        public void someMethod(Double t) {
        }
    }

    public static abstract class AbstractClass<Type> {
        public abstract void someMethod (Type t);
    }

    public static class AbstractClassImpl extends AbstractClass<Double> {
        @Override
        public void someMethod(Double t) {
        }
    }
}