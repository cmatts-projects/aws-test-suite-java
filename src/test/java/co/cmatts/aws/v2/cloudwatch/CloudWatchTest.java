package co.cmatts.aws.v2.cloudwatch;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static co.cmatts.aws.v2.cloudwatch.CloudWatch.*;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CloudWatchTest {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final int NUMBER_METRICS = 100;
    private static final String MY_DIMENSION = "myDimension";
    private static final String MY_COUNT = "myCount";
    private static final String MY_NAMESPACE = "myNamespace";
    private static final String MY_METRIC_NAME = "myMetricName";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @SystemStub
    private static SystemProperties systemProperties;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(CLOUDWATCH);

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY_ID", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_ACCESS_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_STACK_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(null).toString())
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        systemProperties
                .set("software.amazon.awssdk.http.service.impl", "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");
    }

    @Test
    @Order(value = 1)
    void shouldLogMetrics() {
        List<MetricDatum> metrics = new ArrayList<>();
        for (int i = 0; i < NUMBER_METRICS; i++) {
            Instant timestamp = LocalDateTime.now().minusDays(i).toInstant(UTC);
            metrics.add(createMetric(MY_DIMENSION, MY_COUNT, MY_METRIC_NAME, i, timestamp));
        }

        List<PutMetricDataResponse> response = logMetrics(metrics, MY_NAMESPACE);

        int expectedBatches = (NUMBER_METRICS + 24) / 25;
        assertThat(response).hasSize(expectedBatches);

        response.forEach(r -> assertThat(r.sdkHttpResponse().statusCode()).isEqualTo(200));
    }

    @Test
    @Order(value = 2)
    void shouldGetMetrics() {
        assertThat(getAverageForDays(30, MY_DIMENSION, MY_COUNT, MY_NAMESPACE, MY_METRIC_NAME)).isEqualTo(14.5d);
    }
}