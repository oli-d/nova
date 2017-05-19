/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaCommAdapterCreationTest {
    private KafkaCommAdapter.Builder<String> sutBuilder;

    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException {
        this.sutBuilder = KafkaCommAdapter.<String>builder()
                .setMessageMarshaller(message -> message)
                .setMessageUnmarshaller(message -> message)
                .setKafkaObjectFactory(new TestKafkaObjectFactory());
    }

    @Test
    void instanceCannotBeCreatedWithNullMarshaller(){
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sutBuilder.setMessageMarshaller(null).build());
        assertThat(throwable.getMessage(), startsWith("messageMarshaller"));
    }

    @Test
    void testInstanceCannotBeCreatedWithNullUnMarshaller() {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sutBuilder.setMessageUnmarshaller(null).build());
        assertThat(throwable.getMessage(), startsWith("messageUnmarshaller"));
    }

    @Test
    void testInstanceCannotBeCreatedWithNullObjectFactory()  {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sutBuilder.setKafkaObjectFactory(null).build());
        assertThat(throwable.getMessage(), startsWith("kafkaObjectFactory"));
    }

}
