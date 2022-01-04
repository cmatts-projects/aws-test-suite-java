package co.cmatts.aws.cloudwatch;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CloudWatchClientTest {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final int NUMBER_METRICS = 100;

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(CLOUDWATCH);

    private static CloudWatchClient cloudWatchClient;

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_ACCESS_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_STACK_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(null).toString())
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        cloudWatchClient = new CloudWatchClient();
    }

    @Test
    @Order(value = 1)
    void shouldLogMetrics() {
        List<MetricDatum> metrics = new ArrayList<>();
        for (int i = 0; i < NUMBER_METRICS; i++) {
            Date timestamp = Date.from(LocalDateTime.now().minusDays(i).toInstant(UTC));
            metrics.add(cloudWatchClient.createMetric("metricName", i).withTimestamp(timestamp));
        }

        List<PutMetricDataResult> response = cloudWatchClient.logMetrics(metrics);

        int expectedBatches = (NUMBER_METRICS + 24) / 25;
        assertThat(response).hasSize(expectedBatches);

        response.forEach(r -> {
            assertThat(r.getSdkHttpMetadata().getHttpStatusCode()).isEqualTo(200);
        });
    }

    @Test
    @Order(value = 2)
    void shouldGetMetrics() {
        assertThat(cloudWatchClient.getAverageForDays("metricName", 30)).isEqualTo(14.5d);
    }
}