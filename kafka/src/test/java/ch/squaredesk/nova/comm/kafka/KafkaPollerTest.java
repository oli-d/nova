package ch.squaredesk.nova.comm.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaPollerTest {
    private KafkaPoller sut;

    @BeforeEach
    void setup() {
        sut = new KafkaPoller(new StubbedConsumer(), 150, TimeUnit.MILLISECONDS);
    }

    @Test
    void cannotBeInstantiatedWithoutKafkaConsumer() {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sut = new KafkaPoller(null, 1, TimeUnit.SECONDS));
        assertThat(throwable.getMessage(), is("kafkaConsumer must not be null"));
    }

    @Test
    void cannotBeInstantiatedWithNegativePollTimeout() {
        Throwable throwable = assertThrows(IllegalArgumentException.class,
                () -> sut = new KafkaPoller(new StubbedConsumer(), -1, TimeUnit.SECONDS));
        assertThat(throwable.getMessage(), is("pollTimeout must be greater than 0"));
    }

    @Test
    void cannotBeInstantiatedWithZeroPollTimeout() {
        Throwable throwable = assertThrows(IllegalArgumentException.class,
                () -> sut = new KafkaPoller(new StubbedConsumer(), 0, TimeUnit.SECONDS));
        assertThat(throwable.getMessage(), is("pollTimeout must be greater than 0"));
    }

    @Test
    void cannotBeInstantiatedWithoutTimeUnit() {
        Throwable throwable = assertThrows(NullPointerException.class,
                () -> sut = new KafkaPoller(new StubbedConsumer(), 1, null));
        assertThat(throwable.getMessage(), is("pollTimeUnit must not be null"));
    }

    @Test
    void recordsConsumerMustBeSetBeforeStart() {
        Throwable throwable = assertThrows(RuntimeException.class, () -> sut.start());
        assertThat(throwable.getMessage(), containsString("having a valid recordsConsumer"));
    }

    @Test
    void recordsConsumerCannotBeChanged() {
        sut.setRecordsConsumer(records -> {});
        Throwable throwable = assertThrows(RuntimeException.class, () -> sut.setRecordsConsumer(records -> {}));
        assertThat(throwable.getMessage(), containsString("recordsConsumer has already been set"));
    }

    @Test
    void recordsConsumerCannotBeSetToNull() {
        Throwable throwable = assertThrows(NullPointerException.class, () -> sut.setRecordsConsumer(null));
        assertThat(throwable.getMessage(), containsString("recordsConsumer must not be null"));
    }

}