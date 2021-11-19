package co.cmatts.aws.parameterstore;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SSM;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
class ParameterStoreClientTest {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final String PARAMETER_NAME = "MY_PARAMETER";
    private static final String PARAMETER_VALUE = "A parameter value";
    private static final String PARAMETER_DESCRIPTION = "A description";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(SSM);
    private static ParameterStoreClient parameterStoreClient;

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_ACCESS_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_PARAMETER_STORE_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(SSM).toString())
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        parameterStoreClient = new ParameterStoreClient();
    }

    @Test
    void shouldAccessParameter() {
        parameterStoreClient.writeParameter(PARAMETER_NAME, PARAMETER_VALUE, PARAMETER_DESCRIPTION);
        assertThat(parameterStoreClient.readParameter(PARAMETER_NAME)).isEqualTo(PARAMETER_VALUE);
    }
}