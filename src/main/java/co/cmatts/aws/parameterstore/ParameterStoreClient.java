package co.cmatts.aws.parameterstore;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;

public class ParameterStoreClient {

    private static AWSSimpleSystemsManagement client;

    private static AWSSimpleSystemsManagement getParameterStoreClient() {
        if (client != null) {
            return client;
        }

        AWSSimpleSystemsManagementClientBuilder builder = AWSSimpleSystemsManagementClientBuilder.standard();
        String localParameterStoreEndpoint = System.getenv("LOCAL_STACK_ENDPOINT");
        String awsRegion = System.getenv("AWS_REGION");

        if (localParameterStoreEndpoint != null && awsRegion != null) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(localParameterStoreEndpoint, awsRegion));
        }

        client = builder.build();
        return client;
    }

    public static void writeParameter(String parameterName, String parameterValue, String parameterDescription) {
        PutParameterRequest parameterRequest = new PutParameterRequest()
                .withName(parameterName)
                .withValue(parameterValue)
                .withDescription(parameterDescription);

        getParameterStoreClient().putParameter(parameterRequest);
    }

    public static String readParameter(String parameterName) {
        GetParameterRequest parameterRequest = new GetParameterRequest()
                .withName(parameterName);

        return getParameterStoreClient().getParameter(parameterRequest).getParameter().getValue();
    }
}
