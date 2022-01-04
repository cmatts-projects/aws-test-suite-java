package co.cmatts.aws.cloudformation;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;

import java.io.IOException;

public class CloudFormationClient {

    private static AmazonCloudFormation client;

    public static AmazonCloudFormation getCloudformationClient() {
        if (client != null) {
            return client;
        }

        AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard();
        String localCloudWatchEndpoint = System.getenv("LOCAL_STACK_ENDPOINT");
        String awsRegion = System.getenv("AWS_REGION");

        if (localCloudWatchEndpoint != null && awsRegion != null) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(localCloudWatchEndpoint, awsRegion));
        }

        client = builder.build();
        return client;
    }

    public static void createStack(String stackName, String template) throws IOException {
        getCloudformationClient().createStack(createStackRequest(stackName, template));
    }

    private static CreateStackRequest createStackRequest(String stackName, String template) throws IOException {
        String contents = new String(CloudFormationClient.class.getClassLoader()
                .getResourceAsStream(template).readAllBytes());

        return new CreateStackRequest()
                .withStackName(stackName)
                .withTemplateBody(contents);
    }

}
