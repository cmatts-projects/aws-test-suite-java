package co.cmatts.aws.sqs;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import co.cmatts.aws.s3.S3Client;
import software.amazon.payloadoffloading.S3BackedPayloadStore;
import software.amazon.payloadoffloading.S3Dao;

import java.util.List;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

public class SqsClient {

    private final String extendedClientBucket;

    private S3BackedPayloadStore payloadStore;

    private AmazonSQS client;
    private AmazonSQS extendedClient;

    public SqsClient() {
        extendedClientBucket = null;
    }

    public SqsClient(String extendedClientBucket) {
        this.extendedClientBucket = extendedClientBucket;
    }

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

    private AmazonSQS getSqsExtendedClient() {
        if (extendedClientBucket == null) {
            throw new UnsupportedOperationException("Sqs extended client bucket not configured");
        }

        if (extendedClient != null) {
            return extendedClient;
        }

        S3Client s3 = new S3Client();
        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
                        .withPayloadSupportEnabled(s3.getS3Client(), extendedClientBucket);

        extendedClient = new AmazonSQSExtendedClient(getSqsClient(), extendedClientConfig);;
        return extendedClient;
    }

    private S3BackedPayloadStore getPayloadStore() {
        if (payloadStore != null) {
            return payloadStore;
        }
        S3Client s3 = new S3Client();
        S3Dao s3Dao = new S3Dao(s3.getS3Client());
        payloadStore = new S3BackedPayloadStore(s3Dao, extendedClientBucket);

        return payloadStore;
    }

    public void createQueue(String queueName) {
        getSqsClient().createQueue(queueName);
    }

    public void purgeQueue(String queueName) {
        PurgeQueueRequest purgeRequest = new PurgeQueueRequest()
                .withQueueUrl(getQueueUrl(queueName));
        getSqsClient().purgeQueue(purgeRequest);
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
                .collect(toList());
    }

    public void sendToExtendedQueue(String queueName, String message) {
        getSqsExtendedClient().sendMessage(getQueueUrl(queueName), message);
    }

    public void sendToExtendedQueue(String queueName, List<String> messages) {
        List<SendMessageBatchRequestEntry> batchEntries = messages.stream()
                .map(m -> new SendMessageBatchRequestEntry(randomUUID().toString() , m))
                .collect(toList());
        getSqsExtendedClient().sendMessageBatch(getQueueUrl(queueName), batchEntries);
    }

    public List<String> readFromExtendedQueue(String queueName) {
        return getSqsExtendedClient()
                .receiveMessage(getQueueUrl(queueName))
                .getMessages()
                .stream()
                .map(Message::getBody)
                .collect(toList());
    }

    public String toOriginalMessage(String message) {
        return getPayloadStore().getOriginalPayload(message);
    }

    public void deleteOriginalMessage(String message) {
        getPayloadStore().deleteOriginalPayload(message);
    }

    public String storeOriginalMessage(String message) {
        return getPayloadStore().storeOriginalPayload(message, (long)message.getBytes().length);
    }

    private String getQueueUrl(String queueName) {
        return getSqsClient().getQueueUrl(queueName).getQueueUrl();
    }
}
