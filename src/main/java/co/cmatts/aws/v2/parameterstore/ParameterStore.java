package co.cmatts.aws.v2.parameterstore;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

import static co.cmatts.aws.v2.client.Configuration.configureEndPoint;

public class ParameterStore {

    private static SsmClient client;

    private static SsmClient getParameterStoreClient() {
        if (client != null) {
            return client;
        }

        SsmClientBuilder builder = SsmClient.builder();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public static void writeParameter(String parameterName, String parameterValue, String parameterDescription) {
        PutParameterRequest parameterRequest = PutParameterRequest.builder()
                .name(parameterName)
                .value(parameterValue)
                .description(parameterDescription)
                .build();

        getParameterStoreClient().putParameter(parameterRequest);
    }

    public static String readParameter(String parameterName) {
        GetParameterRequest parameterRequest = GetParameterRequest.builder()
                .name(parameterName)
                .build();

        return getParameterStoreClient().getParameter(parameterRequest).parameter().value();
    }
}
