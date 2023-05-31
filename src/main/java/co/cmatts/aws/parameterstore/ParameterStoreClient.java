package co.cmatts.aws.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;

import static co.cmatts.aws.client.Configuration.configureEndPoint;

public class ParameterStoreClient {

    private static AWSSimpleSystemsManagement client;

    private static AWSSimpleSystemsManagement getParameterStoreClient() {
        if (client != null) {
            return client;
        }

        AWSSimpleSystemsManagementClientBuilder builder = AWSSimpleSystemsManagementClientBuilder.standard();
        configureEndPoint(builder);

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
