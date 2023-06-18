package co.cmatts.aws.v1.cloudformation;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.CallExecutorBuilder;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;

import java.io.IOException;

import static co.cmatts.aws.v1.client.Configuration.configureEndPoint;
import static java.time.temporal.ChronoUnit.SECONDS;

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
        waitForStackCreation(stackName);
    }

    private static void waitForStackCreation(String stackName) {
        try {
            RetryConfig retryConfig = new RetryConfigBuilder().withMaxNumberOfTries(30)
                    .withFixedBackoff().withDelayBetweenTries(1, SECONDS)
                    .retryOnSpecificExceptions(RuntimeException.class)
                    .retryOnReturnValue("CREATE_IN_PROGRESS")
                    .build();
            CallExecutor builder = new CallExecutorBuilder()
                    .config(retryConfig)
                    .build();
            builder.execute(() -> describeStacks(stackName).getStacks().stream()
                    .findFirst()
                    .map(Stack::getStackStatus)
                    .orElse("CREATE_IN_PROGRESS"));
        } catch (Exception any) {
            throw new IllegalStateException("Kinesis not started.", any);
        }
    }

    private static DescribeStacksResult describeStacks(String stackName) {
        DescribeStacksRequest describeStacks = new DescribeStacksRequest().withStackName(stackName);
        DescribeStacksResult describeStacksResult = getCloudformationClient().describeStacks(describeStacks);
        return describeStacksResult;
    }

    private static CreateStackRequest createStackRequest(String stackName, String template) throws IOException {
        String contents = new String(CloudFormation.class.getClassLoader()
                .getResourceAsStream(template).readAllBytes());

        return new CreateStackRequest()
                .withStackName(stackName)
                .withTemplateBody(contents);
    }

}
