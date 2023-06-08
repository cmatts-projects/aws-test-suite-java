package co.cmatts.aws.v1.cloudformation;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;

import java.io.IOException;

import static co.cmatts.aws.v1.client.Configuration.configureEndPoint;

public class CloudFormation {

    private static AmazonCloudFormation client;

    public static AmazonCloudFormation getCloudformationClient() {
        if (client != null) {
            return client;
        }

        AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard();
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

        return new CreateStackRequest()
                .withStackName(stackName)
                .withTemplateBody(contents);
    }

}
