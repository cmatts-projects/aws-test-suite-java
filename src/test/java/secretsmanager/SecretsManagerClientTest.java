package secretsmanager;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SECRETSMANAGER;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecretsManagerClientTest {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final String SECRET_NAME = "MY_SECRET";
    private static final String SECRET_VALUE = "{ \"mySecret\": \"mySecretValue\" }";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(SECRETSMANAGER);

    private static SecretsManagerClient secretsManagerClient;

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_ACCESS_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_SECRETS_MANAGER_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(SECRETSMANAGER).toString())
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        secretsManagerClient = new SecretsManagerClient();
    }

    @Test
    @Order(value = 1)
    void shouldCreateSecrets() {
        assertThat(secretsManagerClient.createSecret(SECRET_NAME, SECRET_VALUE)).isNotBlank();
    }

    @Test
    @Order(value = 2)
    void shouldReadSecrets() {
        assertThat(secretsManagerClient.readSecret(SECRET_NAME)).isEqualTo(SECRET_VALUE);
    }

    @Test
    @Order(value = 3)
    void shouldUpdateSecrets() {
        String secret = "{ \"mySecret\": \"mySecretValue\", \"myUpdateSecret\": \"myUpdatedSecretValue\"}";
        secretsManagerClient.updateSecret(SECRET_NAME, secret);

        assertThat(secretsManagerClient.readSecret(SECRET_NAME)).isEqualTo(secret);
    }
}
