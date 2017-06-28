package ch.squaredesk.nova.service.admin;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class AdminCommandServerTest {
    @Test
    void calculateRelativeUrl() {
        String baseUrl = "/admin";
        assertThat(AdminCommandServer.calculateRelativeUrl(baseUrl,"rest://localhost:8888/admin/info"),
                is("/admin/info"));
    }

}