package co.cmatts.aws.v1.s3;

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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;

import static co.cmatts.aws.v1.s3.S3.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
class S3Test {
    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final String TEST_BUCKET = "mybucket";
    private static final String TEST_CONTENT = "{ \"content\": \"some content\" }";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(S3);

    @BeforeAll
    static void beforeAll() {
        environmentVariables
                .set("AWS_ACCESS_KEY", LOCAL_STACK_CONTAINER.getAccessKey())
                .set("AWS_SECRET_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
                .set("LOCAL_STACK_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(null).toString())
                .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        resetS3Client();
        createBucket(TEST_BUCKET);
    }

    @Test
    void shouldCheckBucketExist() {
        assertThat(bucketExists(TEST_BUCKET)).isTrue();
    }

    @Test
    void shouldWriteFileToBucket() throws Exception {
        String s3Url = "s3://mybucket/test/resources/MyFile.txt";
        File localFile = Paths.get(this.getClass().getClassLoader().getResource("MyFile.txt").toURI()).toFile();
        writeToBucket(s3Url, localFile);

        assertThat(fileExists(s3Url)).isTrue();
    }

    @Test
    void shouldWriteStringToBucket() throws Exception {
        String s3Url = "s3://mybucket/test/resources/MyContent.txt";
        writeToBucket(s3Url, TEST_CONTENT);

        assertThat(fileExists(s3Url)).isTrue();
    }

    @Test
    void shouldReadFromBucket() throws Exception {
        String s3Url = "s3://mybucket/test/resources/readFile.txt";
        writeToBucket(s3Url, TEST_CONTENT);

        try(InputStream s3InputStream = readFromBucket(s3Url)) {
            String actualFileContent = new String(s3InputStream.readAllBytes(), UTF_8);
            assertThat(actualFileContent).isEqualTo(TEST_CONTENT);
        }
    }
}