package kinesis;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.services.kinesis.model.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.SdkSystemSetting;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
class KinesisClientTest {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final String MY_STREAM = "myStream";
    private static final String A_MESSAGE = "A message";
    private static final String ANOTHER_MESSAGE = "Another message";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @SystemStub
    private static SystemProperties systemProperties;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(KINESIS);

    private static KinesisClient kinesisClient;

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_ACCESS_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_KINESIS_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(KINESIS).toString())
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        systemProperties
                .set(com.amazonaws.SDKGlobalConfiguration.AWS_CBOR_DISABLE_SYSTEM_PROPERTY, "true")
                .set(SDKGlobalConfiguration.AWS_CBOR_DISABLE_SYSTEM_PROPERTY, "true")
                .set(SdkSystemSetting.CBOR_ENABLED.property(), "false");

        kinesisClient = new KinesisClient();
        kinesisClient.createStream(MY_STREAM, 1);
    }

    @Test
    void shouldBeActiveKinesisStream() {
        assertThat(kinesisClient.getStreamStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldSendAndRetrieveMessageWithStream() {
        kinesisClient.startKinesisListener();
        kinesisClient.sendToKinesis(List.of(A_MESSAGE, ANOTHER_MESSAGE));
        List<Record> records = retrieveRecordsFromKinesis(2);
        kinesisClient.stopKinesisListener();

        List<String> receivedMessages = records.stream()
                .map(r -> UTF_8.decode(r.getData()).toString()).collect(toList());

        assertThat(receivedMessages)
                .containsExactlyInAnyOrder(A_MESSAGE, ANOTHER_MESSAGE);

        List<Record> moreRecords = kinesisClient.getReceivedRecords();
        assertThat(moreRecords).hasSize(0);
    }

    private List<Record> retrieveRecordsFromKinesis(int numberOfRecords) {
        List<Record> records = new ArrayList<>();

        await().atLeast(ONE_HUNDRED_MILLISECONDS)
                .atMost(FIVE_SECONDS)
                .with()
                .pollInterval(ONE_HUNDRED_MILLISECONDS)
                .until(() -> {
                    records.addAll(kinesisClient.getReceivedRecords());
                    return records.size() >= numberOfRecords;
                });

        return records;
    }
}