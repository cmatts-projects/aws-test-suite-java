package co.cmatts.aws.v1.lambda;

import co.cmatts.aws.v1.lambda.model.MyEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleEventHandlerTest {

    @Test
    void shouldReturnEventMessage() {
        SimpleEventHandler handler = new SimpleEventHandler();
        MyEvent event = new MyEvent("A simple lambda response");
        assertThat(handler.handleRequest(event, null)).isEqualTo("A simple lambda response");
    }
}