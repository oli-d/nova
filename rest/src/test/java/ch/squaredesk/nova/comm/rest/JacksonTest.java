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
import com.fasterxml.jackson.databind.SerializationFeature;
import org.glassfish.jersey.server.ManagedAsync;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class JacksonTest {
    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context!=null) context.close();
    }

    @Test
    void jacksonIsEnabledOutOfTheBoxWithAnObjectMapperThatFindsAndRegistersAllModules() throws Exception {
        context = new AnnotationConfigApplicationContext(MyConfigForTest.class);
        MyRestHandler myRestHandler = context.getBean(MyRestHandler.class);
        ObjectMapper om = new ObjectMapper().findAndRegisterModules();
        Person sentPerson = new Person("Lea", "Dotzauer", LocalDate.of(2005,9,16));

        // send entity as JSON
        HttpRequestSender.HttpResponse httpResponse = HttpRequestSender.sendPutRequest("http://localhost:10000/echo", om.writeValueAsString(sentPerson), "application-json");

        // expect that the handler retrieved a properly unmarshalled entity
        assertThat(httpResponse.returnCode, is(200));
        Assertions.assertNotNull(myRestHandler.person);
        assertThat(myRestHandler.person, samePropertyValuesAs(sentPerson));
        Person receivedPerson = om.readValue(httpResponse.replyMessage, Person.class);
        assertThat(receivedPerson, samePropertyValuesAs(sentPerson));
    }

    @Test
    void returnCode400IsSentByJerseyWhenObjectCantBeUnmarshalled() throws Exception {
        context = new AnnotationConfigApplicationContext(MyConfigForTest.class);
        ObjectMapper om = new ObjectMapper();
        Person sentPerson = new Person("Lea", "Dotzauer", LocalDate.of(2005,9,16));

        // send entity as JSON
        HttpRequestSender.HttpResponse httpResponse = HttpRequestSender.sendPutRequest("http://localhost:10000/echo", om.writeValueAsString(sentPerson), "application-json");

        // expect that the handler retrieved a properly unmarshalled entity
        assertThat(httpResponse.returnCode, is(400));
    }

    @Test
    void specificObjectMapperCanBeProvided() throws Exception {
        context = new AnnotationConfigApplicationContext(MyConfigForTestWithSpecificObjectMapper.class);
        MyRestHandler myRestHandler = context.getBean(MyRestHandler.class);
        ObjectMapper om = new ObjectMapper().findAndRegisterModules();
        Person sentPerson = new Person("Lea", "Dotzauer", LocalDate.of(2005, 9, 16));
        String sentPersonAsString = om.writeValueAsString(sentPerson);

        // send entity as JSON
        HttpRequestSender.HttpResponse httpResponse = HttpRequestSender.sendPutRequest("http://localhost:10000/echo", sentPersonAsString, "application-json");

        // expect that the handler retrieved a properly unmarshalled entity
        assertThat(httpResponse.returnCode, is(200));
        Assertions.assertNotNull(myRestHandler.person);
        assertThat(myRestHandler.person, samePropertyValuesAs(sentPerson));
        assertThat(sentPersonAsString, containsString("[2005,9,16]")); // default LocalDateFormat from ObjectMapper
        assertThat(httpResponse.replyMessage, containsString("2005-09-16")); // specific mapper was configured to write ISO
    }


    public static class Person {
        public final String firstName;
        public final String lastName;
        public final LocalDate birthDate;

        @JsonCreator
        public Person(@JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName, @JsonProperty("birthDate") LocalDate birthDate) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.birthDate = birthDate;
        }
    }

    @Configuration
    @Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyConfigForTestWithSpecificObjectMapper {
        @Bean
        public MyRestHandler myRestHandler() {
            return new MyRestHandler();
        }

        @Bean("restObjectMapper")
        public ObjectMapper restObjectMapper() {
            return new ObjectMapper().findAndRegisterModules().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        }
    }

    @Configuration
    @Import({RestEnablingConfiguration.class, NovaProvidingConfiguration.class})
    public static class MyConfigForTest {
        @Bean
        public MyRestHandler myRestHandler() {
            return new MyRestHandler();
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
