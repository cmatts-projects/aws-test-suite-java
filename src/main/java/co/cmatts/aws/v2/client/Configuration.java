package co.cmatts.aws.v2.client;

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

public class Configuration {

    private static final String LOCAL_STACK_ENDPOINT = "LOCAL_STACK_ENDPOINT";
    private static final String AWS_REGION = "AWS_REGION";

    public static AwsClientBuilder configureEndPoint(AwsClientBuilder builder) {
        String localS3Endpoint = System.getenv(LOCAL_STACK_ENDPOINT);
        String awsRegion = System.getenv(AWS_REGION);

        if (localS3Endpoint != null && awsRegion != null) {
            builder.region(Region.of(awsRegion));
            builder.endpointOverride(URI.create(localS3Endpoint));
        }
        return builder;
    }
}
