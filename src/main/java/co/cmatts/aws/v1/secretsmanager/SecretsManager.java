package co.cmatts.aws.v1.secretsmanager;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.CreateSecretResult;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;

import static co.cmatts.aws.v1.client.Configuration.configureEndPoint;

public class SecretsManager {

    private static AWSSecretsManager client;

    private static AWSSecretsManager getSecretsManagerClient() {
        if (client != null) {
            return client;
        }

        AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard();
        configureEndPoint(builder);

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
