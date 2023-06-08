package co.cmatts.aws.v2.secretsmanager;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;

import static co.cmatts.aws.v2.client.Configuration.configureEndPoint;

public class SecretsManager {

    private static SecretsManagerClient client;

    private static SecretsManagerClient getSecretsManagerClient() {
        if (client != null) {
            return client;
        }

        SecretsManagerClientBuilder builder = SecretsManagerClient.builder();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public static String createSecret(String secretName, String secretValue) {
        CreateSecretRequest secretRequest = CreateSecretRequest.builder()
                .name(secretName)
                .secretString(secretValue)
                .build();

        CreateSecretResponse result = getSecretsManagerClient().createSecret(secretRequest);
        return result.arn();
    }

    public static void updateSecret(String secretName, String secretValue) {
        PutSecretValueRequest secretValueRequest = PutSecretValueRequest.builder()
                .secretId(secretName)
                .secretString(secretValue)
                .build();

        getSecretsManagerClient().putSecretValue(secretValueRequest);
    }

    public static String readSecret(String secretName) {
        GetSecretValueRequest secretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        return getSecretsManagerClient().getSecretValue(secretValueRequest).secretString();
    }
}
