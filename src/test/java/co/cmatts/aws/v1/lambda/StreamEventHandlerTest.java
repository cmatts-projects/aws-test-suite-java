package co.cmatts.aws.v1.lambda;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class StreamEventHandlerTest {

    private static final String JSON_PAYLOAD = "{ \"payload\": \"a streamed payload\" }";

    @Test
    void shouldOutputEventMessageStream() throws Exception {
        StreamEventHandler handler = new StreamEventHandler();
        InputStream inputStream = new ByteArrayInputStream(JSON_PAYLOAD.getBytes());
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            handler.handleRequest(inputStream, outputStream, null);
            assertThat(outputStream.toString()).isEqualTo(JSON_PAYLOAD);
        }
    }

}