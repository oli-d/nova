/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.rest;

import ch.squaredesk.net.PortFinder;
import ch.squaredesk.nova.autoconfigure.comm.http.*;
import ch.squaredesk.nova.autoconfigure.comm.rest.RestAutoConfig;
import ch.squaredesk.nova.autoconfigure.core.NovaAutoConfiguration;
import ch.squaredesk.nova.comm.http.HttpRequestSender;
import ch.squaredesk.nova.comm.http.HttpRequestSender.RequestHeaders;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.glassfish.jersey.server.ManagedAsync;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.Objects;

public class JacksonTest {
    private ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestAutoConfig.class, HttpAdapterAutoConfig.class, HttpServerAutoConfig.class, HttpClientAutoConfig.class, NovaAutoConfiguration.class));

    @Test
    void jacksonIsEnabledOutOfTheBoxWithAnObjectMapperThatFindsAndRegistersAllModules() {
        applicationContextRunner
                .withUserConfiguration(MyConfigForTest.class)
                .withPropertyValues("nova.http.server.port=" + PortFinder.findFreePort())
                .run(context -> {
                    HttpServerConfigurationProperties httpServerConfigurationProperties = context.getBean(HttpServerConfigurationProperties.class);
                    ObjectMapper om = new ObjectMapper().findAndRegisterModules();
                    Person sentPerson = new Person("Lea", "Dotzauer", LocalDate.of(2005,9,16));

                    // send entity as JSON
                    HttpRequestSender.HttpResponse httpResponse = HttpRequestSender.sendPutRequest("http://localhost:" + httpServerConfigurationProperties.getPort() +"/echo",
                            om.writeValueAsString(sentPerson), RequestHeaders.createForContentType("application/json"));

                    // expect that the handler retrieved a properly unmarshalled entity
                    MatcherAssert.assertThat(httpResponse.returnCode, Matchers.is(200));
                    Assertions.assertNotNull(MyRestHandler.person);
                    MatcherAssert.assertThat(MyRestHandler.person, Matchers.is(sentPerson));
                    Person receivedPerson = om.readValue(httpResponse.replyMessage, Person.class);
                    MatcherAssert.assertThat(receivedPerson, Matchers.is(sentPerson));
                });
    }

    @Test
    void returnCode400IsSentByJerseyWhenObjectCantBeUnmarshalled() {
        applicationContextRunner
                .withUserConfiguration(MyConfigForTest.class)
                .run(context -> {
                    HttpServerConfigurationProperties httpServerConfigurationProperties = context.getBean(HttpServerConfigurationProperties.class);
                    ObjectMapper om = new ObjectMapper();
                    Person sentPerson = new Person("Lea", "Dotzauer", LocalDate.of(2005,9,16));

                    // send entity as JSON
                    HttpRequestSender.HttpResponse httpResponse = HttpRequestSender.sendPutRequest("http://localhost:" + httpServerConfigurationProperties.getPort() + "/echo",
                            om.writeValueAsString(sentPerson), RequestHeaders.createForContentType("application/json"));

                    // expect that the handler retrieved a properly unmarshalled entity
                    MatcherAssert.assertThat(httpResponse.returnCode, Matchers.is(400));
                });
    }

    @Test
    void specificObjectMapperCanBeProvided() {
        applicationContextRunner
                .withUserConfiguration(MyConfigForTestWithSpecificObjectMapper.class)
                .run(context -> {
                    HttpServerConfigurationProperties httpServerConfigurationProperties = context.getBean(HttpServerConfigurationProperties.class);
                    ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
                    Person sentPerson = new Person("Lea", "Dotzauer", LocalDate.of(2005, 9, 16));
                    String sentPersonAsString = objectMapper.writeValueAsString(sentPerson);

                    // send entity as JSON
                    HttpRequestSender.HttpResponse httpResponse = HttpRequestSender.sendPutRequest("http://localhost:" + httpServerConfigurationProperties.getPort() + "/echo",
                            sentPersonAsString, RequestHeaders.createForContentType("application/json"));

                    // expect that the handler retrieved a properly unmarshalled entity
                    MatcherAssert.assertThat(httpResponse.returnCode, Matchers.is(200));
                    Assertions.assertNotNull(MyRestHandler.person);
                    MatcherAssert.assertThat(MyRestHandler.person, Matchers.is(sentPerson));
                    MatcherAssert.assertThat(sentPersonAsString, Matchers.containsString("2005-09-16")); // specific mapper was configured to write ISO
                    MatcherAssert.assertThat(httpResponse.replyMessage, Matchers.containsString("2005-09-16")); // specific mapper was configured to write ISO
                });
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return Objects.equals(firstName, person.firstName) &&
                    Objects.equals(lastName, person.lastName) &&
                    Objects.equals(birthDate, person.birthDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, lastName, birthDate);
        }
    }

    @Configuration
    public static class MyConfigForTestWithSpecificObjectMapper {
        @Bean(BeanIdentifiers.OBJECT_MAPPER)
        public ObjectMapper restObjectMapper() {
            return new ObjectMapper().findAndRegisterModules().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        }
        @Bean
        MyRestHandler myRestHandler() {
            return new MyRestHandler();
        }
    }

    @Configuration
    public static class MyConfigForTest {
        @Bean
        MyRestHandler myRestHandler() {
            return new MyRestHandler();
        }
    }

    @Path("/echo")
    public static class MyRestHandler {
        static Person person;

        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @ManagedAsync
        public void echo (Person person, @Suspended AsyncResponse response) {
            MyRestHandler.person = person;
            response.resume(person);
        }
    }

}
