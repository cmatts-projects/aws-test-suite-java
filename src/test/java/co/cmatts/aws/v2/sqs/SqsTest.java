package co.cmatts.aws.v2.sqs;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.util.ArrayList;
import java.util.List;

import static co.cmatts.aws.v1.s3.S3.createBucket;
import static co.cmatts.aws.v1.s3.S3.resetS3Client;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
class SqsTest {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final String TEST_QUEUE_BUCKET = "my-queue-bucket";
    private static final String TEST_QUEUE = "myQueue";
    private static final String TEST_MESSAGE = "A test message";
    private static final String EXTENDED_MESSAGE_PAYLOAD = "\\[\"awsv2.repackaged.software.amazon.payloadoffloading.PayloadS3Pointer\",\\{\"s3BucketName\":\"my-queue-bucket\",\"s3Key\":\".*\"\\}\\]";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @SystemStub
    private static SystemProperties systemProperties;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(SQS, S3);

    private static Sqs sqs;

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY_ID", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_ACCESS_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_STACK_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(null).toString())
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        systemProperties
                .set("software.amazon.awssdk.http.service.impl", "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");

        resetS3Client();
        createBucket(TEST_QUEUE_BUCKET);
        sqs = new Sqs(TEST_QUEUE_BUCKET);
        sqs.createQueue(TEST_QUEUE);
    }

    @BeforeEach
    void purgeQueue() {
        sqs.purgeQueue(TEST_QUEUE);
    }

    @Test
    void shouldSendToQueue() {
        sqs.sendToQueue(TEST_QUEUE, TEST_MESSAGE);

        List<String> receivedMessages = sqs.readFromQueue(TEST_QUEUE);
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0)).isEqualTo(TEST_MESSAGE);
    }

    @Test
    void shouldSendSimpleMessageToExtendedQueue() {
        sqs.sendToExtendedQueue(TEST_QUEUE, TEST_MESSAGE);

        List<String> receivedMessages = sqs.readFromQueue(TEST_QUEUE);
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0)).isEqualTo(TEST_MESSAGE);
    }

    @Test
    void shouldSendLargeMessageToExtendedQueue() {
        String largeMessage = StringUtils.repeat("X", 257 * 1024);
        sqs.sendToExtendedQueue(TEST_QUEUE, largeMessage);

        List<String> receivedMessages = sqs.readFromQueue(TEST_QUEUE);
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0)).matches(EXTENDED_MESSAGE_PAYLOAD);
    }

    @Test
    void shouldReceiveLargeMessageFromExtendedQueue() {
        String largeMessage = StringUtils.repeat("X", 257 * 1024);
        sqs.sendToExtendedQueue(TEST_QUEUE, largeMessage);

        List<String> receivedMessages = sqs.readFromExtendedQueue(TEST_QUEUE);
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0)).matches(largeMessage);
    }

    @Test
    void shouldSendBigMessageBatchToExtendedQueue() {
        String largeMessage = StringUtils.repeat("X", 250 * 1024);
        List<String> messageBatch = List.of(largeMessage, largeMessage, largeMessage);
        sqs.sendToExtendedQueue(TEST_QUEUE, messageBatch);

        List<String> receivedMessages = retrieveMessagesFromSqs(3);
        assertThat(receivedMessages.get(0)).matches(largeMessage);
    }

    private List<String> retrieveMessagesFromSqs(int numberOfRecords) {
        List<String> messages = new ArrayList<>();

        await().atLeast(ONE_HUNDRED_MILLISECONDS)
                .atMost(FIVE_SECONDS)
                .with()
                .pollInterval(ONE_HUNDRED_MILLISECONDS)
                .until(() -> {
                    messages.addAll(sqs.readFromQueue(TEST_QUEUE));
                    return messages.size() >= numberOfRecords;
                });

        return messages;
    }
}