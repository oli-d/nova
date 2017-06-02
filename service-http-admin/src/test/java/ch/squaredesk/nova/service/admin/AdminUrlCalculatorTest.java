package ch.squaredesk.nova.service.admin;

import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.Port;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AdminUrlCalculatorTest {
    private static final String HOST = "localhost";
    private static final String BASE_URL = "/admin";
    private static final int PORT = 8888;

    private AdminUrlCalculator sut;

    private AdminUrlCalculator createSut(String host, String baseUrl, int port) {
        return new AdminUrlCalculator(host, baseUrl, port);
    }

    @BeforeEach
    void setup() {
        sut = createSut(HOST, BASE_URL, PORT);
    }

    @Test
    void constructorNeedsHostName() {
    }

    @Test
    void constructorNeedsBaseUrlName() {
    }

    private AdminCommandConfig createAdminCommandConfig() throws Exception {
        return new AdminCommandConfig(new Object(),
                Object.class.getDeclaredMethod("equals", Object.class),
                "a", "b", "c");
    }

    @Test
    void urlCalculatedAsExpected() throws Exception {
        AdminCommandConfig config = createAdminCommandConfig();
        assertThat(sut.urlFor(config), is("http://localhost:8888/admin/equals"));
    }

    @Test
    void baseUrlEnhancedIfItDoesntStartWithLeadingSlash() throws Exception {
        sut = createSut(HOST, "withoutSlash", 1234);
        AdminCommandConfig config = createAdminCommandConfig();
        assertThat(sut.urlFor(config), is("http://localhost:1234/withoutSlash/equals"));
    }

}