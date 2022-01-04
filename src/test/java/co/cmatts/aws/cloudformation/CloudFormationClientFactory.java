package co.cmatts.aws.cloudformation;

import com.amazonaws.PredefinedClientConfigurations;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import org.testcontainers.containers.localstack.LocalStackContainer;

public class CloudFormationClientFactory {
    private final LocalStackContainer localStackContainer;

    public CloudFormationClientFactory(LocalStackContainer localStackContainer) {
        this.localStackContainer = localStackContainer;
    }

    public AmazonCloudFormation createCloudformationClient() {
        String cfEndpoint = localStackContainer.getEndpointOverride(null).toString();

        return AmazonCloudFormationClientBuilder.standard()
                .withClientConfiguration(PredefinedClientConfigurations.defaultConfig())
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(cfEndpoint,
                        localStackContainer.getRegion()))
                .build();
    }
}
