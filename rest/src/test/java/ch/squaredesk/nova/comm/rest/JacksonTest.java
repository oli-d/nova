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

package ch.squaredesk.nova.comm.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

public class JacksonTest {
    @Test
    void jacksonIsEnabledOutOfTheBox() {
        // create echo server

        // send entity as JSON

        // expect the entity back
    }

    public static class Person {
        public final String firstName;
        public final String lastName;

        @JsonCreator
        public Person(@JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}
