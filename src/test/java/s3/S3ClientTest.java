package s3;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class S3ClientTest {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final String TEST_BUCKET = "mybucket";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(S3);

    private static S3Client s3Client;

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_ACCESS_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_S3_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(S3).toString())
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        s3Client = new S3Client();
    }

    @Test
    @Order(value = 1)
    void shouldCreateBucket() {
        s3Client.createBucket(TEST_BUCKET);

        assertThat(s3Client.bucketExists(TEST_BUCKET)).isTrue();
    }

    @Test
    @Order(value = 2)
    void shouldWriteToBucket() throws Exception {
        String s3Url = "s3://mybucket/test/resources/MyFile.txt";
        File localFile = Paths.get(this.getClass().getClassLoader().getResource("MyFile.txt").toURI()).toFile();
        s3Client.writeToBucket(s3Url, localFile);

        assertThat(s3Client.fileExists(s3Url)).isTrue();
    }

    @Test
    @Order(value = 3)
    void shouldReadFromBucket() throws Exception {
        String s3Url = "s3://mybucket/test/resources/MyFile.txt";
        InputStream expectedInputStream = this.getClass().getClassLoader().getResourceAsStream("MyFile.txt");
        String expectedFileContent = new String(expectedInputStream.readAllBytes(), UTF_8);

        try(InputStream s3InputStream = s3Client.readFromBucket(s3Url)) {
            String actualFileContent = new String(s3InputStream.readAllBytes(), UTF_8);
            assertThat(actualFileContent).isEqualTo(expectedFileContent);
        }
    }
}