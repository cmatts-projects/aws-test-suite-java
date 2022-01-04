package co.cmatts.aws.lambda;

import co.cmatts.aws.sqs.SqsClient;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.ArrayList;
import java.util.List;

import static co.cmatts.aws.s3.S3Client.createBucket;
import static co.cmatts.aws.s3.S3Client.resetS3Client;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
class LargeSqsEventHandlerTest {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final String TEST_QUEUE_BUCKET = "my-queue-bucket";
    private static final String TEST_QUEUE = "myQueue";
    private static final String TEST_MESSAGE = StringUtils.repeat("X", 257 * 1024);

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(SQS, S3);

    private static SqsClient sqsClient;

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_ACCESS_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_STACK_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(null).toString())
                .set("FORWARD_QUEUE", TEST_QUEUE)
                .set("EXTENDED_CLIENT_BUCKET", TEST_QUEUE_BUCKET)
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        resetS3Client();
        createBucket(TEST_QUEUE_BUCKET);
        sqsClient = new SqsClient(TEST_QUEUE_BUCKET);
        sqsClient.createQueue(TEST_QUEUE);
    }

    @Test
    void shouldReturnEventMessage() {
        LargeSqsEventHandler handler = new LargeSqsEventHandler();
        SQSEvent event = createSqsEvent();
        handler.handleRequest(event, null);

        List<String> receivedMessages = retrieveMessagesFromSqs(3);
        receivedMessages.stream().forEach(message -> assertThat(message).isEqualTo(TEST_MESSAGE));
    }

    private SQSEvent createSqsEvent() {
        SQSEvent event = new SQSEvent();
        event.setRecords(createSqsMessages());
        return event;
    }

    private List<SQSMessage> createSqsMessages() {
        return List.of(createSqsMessage(), createSqsMessage(), createSqsMessage());
    }

    private SQSMessage createSqsMessage() {
        String messageId = randomUUID().toString();
        SQSMessage sqsMessage = new SQSMessage();
        sqsMessage.setMessageId(messageId);
        sqsMessage.setBody(sqsClient.storeOriginalMessage(TEST_MESSAGE));
        return sqsMessage;
    }

    private List<String> retrieveMessagesFromSqs(int numberOfRecords) {
        List<String> messages = new ArrayList<>();

        await().atLeast(ONE_HUNDRED_MILLISECONDS)
                .atMost(FIVE_SECONDS)
                .with()
                .pollInterval(ONE_HUNDRED_MILLISECONDS)
                .until(() -> {
                    messages.addAll(sqsClient.readFromExtendedQueue(TEST_QUEUE));
                    return messages.size() >= numberOfRecords;
                });

        return messages;
    }
}