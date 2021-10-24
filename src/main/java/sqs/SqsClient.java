package sqs;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;

import java.util.List;
import java.util.stream.Collectors;

public class SqsClient {

    private AmazonSQS client;

    private AmazonSQS getSqsClient() {
        if (client != null) {
            return client;
        }

        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
        String localSqsEndpoint = System.getenv("LOCAL_SQS_ENDPOINT");
        String awsRegion = System.getenv("AWS_REGION");

        if (localSqsEndpoint != null && awsRegion != null) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(localSqsEndpoint, awsRegion));
        }

        client = builder.build();
        return client;
    }

    public String createQueue(String queueName) {
        return getSqsClient().createQueue(queueName).getQueueUrl();
    }

    public void sendToQueue(String queueName, String message) {
        getSqsClient().sendMessage(getQueueUrl(queueName), message);
    }

    public List<String> readFromQueue(String queueName) {
        return getSqsClient()
                .receiveMessage(getQueueUrl(queueName))
                .getMessages()
                .stream()
                .map(Message::getBody)
                .collect(Collectors.toList());
    }

    private String getQueueUrl(String queueName) {
        return getSqsClient().getQueueUrl(queueName).getQueueUrl();
    }
}
