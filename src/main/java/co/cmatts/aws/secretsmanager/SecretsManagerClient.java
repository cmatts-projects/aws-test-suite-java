package co.cmatts.aws.secretsmanager;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.CreateSecretResult;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;

public class SecretsManagerClient {

    private static AWSSecretsManager client;

    private static AWSSecretsManager getSecretsManagerClient() {
        if (client != null) {
            return client;
        }

        AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard();
        String localSecretsManagerEndpoint = System.getenv("LOCAL_STACK_ENDPOINT");
        String awsRegion = System.getenv("AWS_REGION");

        if (localSecretsManagerEndpoint != null && awsRegion != null) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(localSecretsManagerEndpoint, awsRegion));
        }

        client = builder.build();
        return client;
    }

    public static String createSecret(String secretName, String secretValue) {
        CreateSecretRequest secretRequest = new CreateSecretRequest()
                .withName(secretName)
                .withSecretString(secretValue);

        CreateSecretResult result = getSecretsManagerClient().createSecret(secretRequest);
        return result.getARN();
    }

    public static void updateSecret(String secretName, String secretValue) {
        PutSecretValueRequest secretValueRequest = new PutSecretValueRequest()
                .withSecretId(secretName)
                .withSecretString(secretValue);

        getSecretsManagerClient().putSecretValue(secretValueRequest);
    }

    public static String readSecret(String secretName) {
        GetSecretValueRequest secretValueRequest = new GetSecretValueRequest()
                .withSecretId(secretName);

        return getSecretsManagerClient().getSecretValue(secretValueRequest).getSecretString();
    }
}
