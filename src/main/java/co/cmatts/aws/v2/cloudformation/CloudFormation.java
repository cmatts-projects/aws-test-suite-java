package co.cmatts.aws.v2.cloudformation;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClientBuilder;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;

import java.io.IOException;

import static co.cmatts.aws.v2.client.Configuration.configureEndPoint;

public class CloudFormation {

    private static CloudFormationClient client;

    public static CloudFormationClient getCloudformationClient() {
        if (client != null) {
            return client;
        }

        CloudFormationClientBuilder builder = CloudFormationClient.builder();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public static void createStack(String stackName, String template) throws IOException {
        getCloudformationClient().createStack(createStackRequest(stackName, template));
    }

    private static CreateStackRequest createStackRequest(String stackName, String template) throws IOException {
        String contents = new String(CloudFormation.class.getClassLoader()
                .getResourceAsStream(template).readAllBytes());

        return CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(contents)
                .build();
    }

}
