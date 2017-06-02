package ch.squaredesk.nova.service.admin;

import ch.squaredesk.nova.service.admin.messages.Info;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AdminInfoCenterTest {
    AdminInfoCenter sut;
    AdminUrlCalculator urlCalculator;

    @BeforeEach
    void setup() {
        urlCalculator = new AdminUrlCalculator("host","admin", 1234);
        sut = new AdminInfoCenter(urlCalculator);
    }

    @Test
    void properInfoIsReturnedEvenIfNothingRegisteredYet() {
        Info info = sut.getInfo();
        assertNotNull(info);
        assertThat(info.supportedCommands.length, is(0));
    }

    @Test
    void configsCanBeRegistered() throws Exception {
        class MyClass {
            public String m1 (String s, String s2) { return ""; }
            public String m2 (String s, String s2, String s3) { return ""; }
        }
        MyClass x = new MyClass();
        Method m1 = MyClass.class.getDeclaredMethod("m1", String.class, String.class);
        Method m2 = MyClass.class.getDeclaredMethod("m2", String.class, String.class, String.class);
        AdminCommandConfig adminCommandConfig1 = new AdminCommandConfig(x, m1, "one", "two");
        AdminCommandConfig adminCommandConfig2 = new AdminCommandConfig(x, m2, "a", "b", "c");
        sut.register(adminCommandConfig1);
        sut.register(adminCommandConfig2);

        List<AdminCommandConfig> configs = sut.getConfigs();
        assertThat(configs.size(), is(2));
        assertThat(configs, contains(adminCommandConfig1, adminCommandConfig2));
    }

    @Test
    void properInfoIsReturned() throws Exception {
        class MyClass {
            public String m1 (String s, String s2) { return ""; }
            public String m2 (String s, String s2, String s3) { return ""; }
        }
        MyClass x = new MyClass();
        Method m1 = MyClass.class.getDeclaredMethod("m1", String.class, String.class);
        Method m2 = MyClass.class.getDeclaredMethod("m2", String.class, String.class, String.class);
        AdminCommandConfig adminCommandConfig1 = new AdminCommandConfig(x, m1, "one", "two");
        AdminCommandConfig adminCommandConfig2 = new AdminCommandConfig(x, m2, "a", "b", "c");
        sut.register(adminCommandConfig1);
        sut.register(adminCommandConfig2);

        Info info = sut.getInfo();
        assertNotNull(info);
        assertThat(info.supportedCommands.length, is(2));
        assertThat(info.supportedCommands[0].url, is(urlCalculator.urlFor(adminCommandConfig1)));
        assertThat(info.supportedCommands[0].parameters.length, is(2));
        assertThat(info.supportedCommands[0].parameters[0], is("one"));
        assertThat(info.supportedCommands[0].parameters[1], is("two"));
        assertThat(info.supportedCommands[1].url, is(urlCalculator.urlFor(adminCommandConfig2)));
        assertThat(info.supportedCommands[1].parameters.length, is(3));
        assertThat(info.supportedCommands[1].parameters[0], is("a"));
        assertThat(info.supportedCommands[1].parameters[1], is("b"));
        assertThat(info.supportedCommands[1].parameters[2], is("c"));
    }
}