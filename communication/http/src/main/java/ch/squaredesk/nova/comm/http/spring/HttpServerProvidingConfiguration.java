/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.comm.http.HttpServerFactory;
import ch.squaredesk.nova.comm.http.HttpServerSettings;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Optional;

@Configuration
public class HttpServerProvidingConfiguration {
    public interface BeanIdentifiers {
        String PORT = "NOVA.HTTP.SERVER.PORT";
        String INTERFACE = "NOVA.HTTP.SERVER.INTERFACE";
        String TRUST_STORE = "NOVA.HTTP.SERVER.TRUST_STORE";
        String TRUST_STORE_PASSWORD = "NOVA.HTTP.SERVER.TRUST_STORE_PASSWORD";
        String TRUST_STORE_PASSWORD_FILE = "NOVA.HTTP.SERVER.TRUST_STORE_PASSWORD_FILE";
        String KEY_STORE = "NOVA.HTTP.SERVER.KEY_STORE";
        String KEY_STORE_PASSWORD = "NOVA.HTTP.SERVER.KEY_STORE_PASSWORD";
        String KEY_STORE_PASSWORD_FILE = "NOVA.HTTP.SERVER.KEY_STORE_PASSWORD_FILE";
        String AUTO_START_SERVER = "NOVA.HTTP.SERVER.AUTO_START";
        String AUTO_CREATE_SERVER = "NOVA.HTTP.SERVER.AUTO_CREATE";

        String SETTINGS = "NOVA.HTTP.SERVER.SETTINGS";
        String SERVER = "NOVA.HTTP.SERVER";
        String SERVER_NOTIFIER = "NOVA.HTTP.SERVER.NOTIFIER";
        String SERVER_STARTER = "NOVA.HTTP.SERVER.STARTER";
    }

    @Bean(BeanIdentifiers.PORT)
    int httpServerPort(Environment environment) {
        return environment.getProperty(BeanIdentifiers.PORT, Integer.class, 10000);
    }

    @Bean(BeanIdentifiers.INTERFACE)
    String interfaceName(Environment environment) {
        return environment.getProperty(BeanIdentifiers.INTERFACE, "0.0.0.0");
    }

    @Bean(BeanIdentifiers.TRUST_STORE)
    String sslTrustStore(Environment environment) {
        return environment.getProperty(BeanIdentifiers.TRUST_STORE);
    }

    @Bean(BeanIdentifiers.TRUST_STORE_PASSWORD)
    String sslTrustStorePassword(Environment environment) {
        return environment.getProperty(BeanIdentifiers.TRUST_STORE_PASSWORD);
    }

    @Bean(BeanIdentifiers.TRUST_STORE_PASSWORD_FILE)
    String sslTrustStorePaasswordFile(Environment environment) {
        return environment.getProperty(BeanIdentifiers.TRUST_STORE_PASSWORD_FILE);
    }

    @Bean(BeanIdentifiers.KEY_STORE)
    String sslKeyStore(Environment environment) {
        return environment.getProperty(BeanIdentifiers.KEY_STORE);
    }

    @Bean(BeanIdentifiers.KEY_STORE_PASSWORD)
    String sslKeyStorePassword(Environment environment) {
        return environment.getProperty(BeanIdentifiers.KEY_STORE_PASSWORD);
    }

    @Bean(BeanIdentifiers.KEY_STORE_PASSWORD_FILE)
    String sslKeyStorePasswordFile(Environment environment) {
        return environment.getProperty(BeanIdentifiers.KEY_STORE_PASSWORD_FILE);
    }

    @Bean(BeanIdentifiers.SETTINGS)
    HttpServerSettings httpServerSettings(
            @Autowired(required = false) Nova nova,
            @Qualifier(BeanIdentifiers.INTERFACE) String interfaceName,
            @Qualifier(BeanIdentifiers.PORT) int httpServerPort,
            @Qualifier(BeanIdentifiers.TRUST_STORE) @Autowired(required = false) String sslTrustStore,
            @Qualifier(BeanIdentifiers.TRUST_STORE_PASSWORD) @Autowired(required = false) String sslTrustStorePassword,
            @Qualifier(BeanIdentifiers.TRUST_STORE_PASSWORD_FILE) @Autowired(required = false) String sslTrustStorePasswordFile,
            @Qualifier(BeanIdentifiers.KEY_STORE) @Autowired(required = false) String sslKeyStore,
            @Qualifier(BeanIdentifiers.KEY_STORE_PASSWORD) @Autowired(required = false) String sslKeyStorePassword,
            @Qualifier(BeanIdentifiers.KEY_STORE_PASSWORD_FILE) @Autowired(required = false) String sslKeyStorePasswordFile) {

        String trustStorePassword = Optional.ofNullable(sslTrustStorePasswordFile)
                .map(passwordFile -> PasswordFileReader.readPasswordFromFile(nova, passwordFile))
                .orElse(sslTrustStorePassword);

        String keyStorePassword = Optional.ofNullable(sslKeyStorePasswordFile)
                .map(passwordFile -> PasswordFileReader.readPasswordFromFile(nova, passwordFile))
                .orElse(sslKeyStorePassword);


        return HttpServerSettings.builder()
                .interfaceName(interfaceName)
                .port(httpServerPort)
                .sslKeyStorePath(sslKeyStore)
                .sslKeyStorePass(keyStorePassword)
                .sslTrustStorePath(sslTrustStore)
                .sslTrustStorePass(trustStorePassword)
                .build();
    }

    @Bean(BeanIdentifiers.SERVER)
    HttpServer httpServer(@Qualifier(BeanIdentifiers.AUTO_CREATE_SERVER) boolean autoCreateHttpServer,
                          @Qualifier(BeanIdentifiers.SETTINGS) @Autowired(required = false) HttpServerSettings httpServerSettings) {
        if (autoCreateHttpServer) {
            return HttpServerFactory.serverFor(httpServerSettings);
        } else {
            return null;
        }
    }

    @Bean(BeanIdentifiers.AUTO_START_SERVER)
    public boolean autoStartHttpServer(Environment environment) {
        return environment.getProperty(BeanIdentifiers.AUTO_START_SERVER, Boolean.class, true);
    }

    @Bean(BeanIdentifiers.AUTO_CREATE_SERVER)
    public boolean autoCreateHttpServer(Environment environment) {
        return environment.getProperty(BeanIdentifiers.AUTO_CREATE_SERVER, Boolean.class, true);
    }

    @Bean(BeanIdentifiers.SERVER_STARTER)
    public HttpServerStarter httpServerStarter(@Qualifier(BeanIdentifiers.AUTO_START_SERVER) boolean autoStartHttpServer,
                                               @Qualifier(BeanIdentifiers.SERVER) @Autowired(required = false) HttpServer httpServer) {
        return new HttpServerStarter(httpServer, autoStartHttpServer);
    }

    @Bean("autoNotifyAboutHttpServerAvailability")
    public boolean autoNotifyAboutHttpServerAvailability() {
        return true;
    }

    @Bean(BeanIdentifiers.SERVER_NOTIFIER)
    public HttpServerBeanNotifier httpServerBeanNotifier(@Qualifier("autoNotifyAboutHttpServerAvailability") boolean autoNotifyAboutHttpServerAvailability) {
        return new HttpServerBeanNotifier(autoNotifyAboutHttpServerAvailability);
    }


}