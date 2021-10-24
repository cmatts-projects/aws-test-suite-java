package sqs;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SqsClientTest {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final String TEST_QUEUE = "myQueue";
    private static final String TEST_MESSAGE = "A test message";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(SQS);

    private static SqsClient sqsClient;

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_ACCESS_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_SQS_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(SQS).toString())
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        sqsClient = new SqsClient();
    }

    @Test
    @Order(value = 1)
    void shouldCreateQueue() {
        String queueUrl = sqsClient.createQueue(TEST_QUEUE);
        assertThat(queueUrl).isNotBlank();
    }

    @Test
    @Order(value = 2)
    void shouldSendToQueue() {
        sqsClient.sendToQueue(TEST_QUEUE, TEST_MESSAGE);

        List<String> receivedMessages = sqsClient.readFromQueue(TEST_QUEUE);
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0)).isEqualTo(TEST_MESSAGE);
    }
}