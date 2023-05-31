package co.cmatts.aws.v1.client;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsSyncClientBuilder;

public class Configuration {

    private static final String LOCAL_STACK_ENDPOINT = "LOCAL_STACK_ENDPOINT";
    private static final String AWS_REGION = "AWS_REGION";

    public static void configureEndPoint(AwsSyncClientBuilder builder) {
        String localS3Endpoint = System.getenv(LOCAL_STACK_ENDPOINT);
        String awsRegion = System.getenv(AWS_REGION);

        if (localS3Endpoint != null && awsRegion != null) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(localS3Endpoint, awsRegion));
        }
    }
}
