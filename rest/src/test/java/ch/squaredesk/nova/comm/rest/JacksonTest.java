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

import ch.squaredesk.nova.comm.http.HttpRequestSender;
import ch.squaredesk.nova.spring.NovaProvidingConfiguration;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ManagedAsync;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class JacksonTest {
    @Test
    void jacksonIsEnabledOutOfTheBox() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfigForTest.class);
        MyRestHandler myRestHandler = ctx.getBean(MyRestHandler.class);
        ObjectMapper om = new ObjectMapper();
        Person sentPerson = new Person("Lea", "Dotzauer");

        // send entity as JSON
        HttpRequestSender.HttpResponse httpResponse = HttpRequestSender.sendPutRequest("http://localhost:10000/echo", om.writeValueAsString(sentPerson), "application-json");

        // expect that the handler retrieved a properly unmarshalled entity
        assertThat(httpResponse.returnCode, is(200));
        Assertions.assertNotNull(myRestHandler.person);
        assertThat(myRestHandler.person, samePropertyValuesAs(sentPerson));
        Person receivedPerson = om.readValue(httpResponse.replyMessage, Person.class);
        assertThat(receivedPerson, samePropertyValuesAs(sentPerson));

        // tear down
        ctx.close();
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

    @Configuration
    @Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyConfigForTest {
        @Bean
        public MyRestHandler myRestHandler() {
            return new MyRestHandler();
        }

        @Bean("restObjectMapper")
        public ObjectMapper restObjectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Path("/echo")
    public static class MyRestHandler {
        public Person person;

        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @ManagedAsync
        public void echo (Person person, @Suspended AsyncResponse response) {
            this.person = person;
            response.resume(person);
        }
    }
}
