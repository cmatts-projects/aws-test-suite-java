package co.cmatts.aws.v2.sqs;

import awsv2.repackaged.com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import awsv2.repackaged.com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import awsv2.repackaged.software.amazon.payloadoffloading.S3BackedPayloadStore;
import awsv2.repackaged.software.amazon.payloadoffloading.S3Dao;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.*;

import java.lang.UnsupportedOperationException;
import java.util.List;

import static co.cmatts.aws.v2.client.Configuration.configureEndPoint;
import static co.cmatts.aws.v2.s3.S3.getS3Client;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

public class Sqs {

    private final String extendedClientBucket;

    private S3BackedPayloadStore payloadStore;

    private SqsClient client;
    private SqsClient extendedClient;

    public Sqs() {
        extendedClientBucket = null;
    }

    public Sqs(String extendedClientBucket) {
        this.extendedClientBucket = extendedClientBucket;
    }

    private SqsClient getSqsClient() {
        if (client != null) {
            return client;
        }

        SqsClientBuilder builder = SqsClient.builder();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    private SqsClient getSqsExtendedClient() {
        if (extendedClientBucket == null) {
            throw new UnsupportedOperationException("Sqs extended client bucket not configured");
        }

        if (extendedClient != null) {
            return extendedClient;
        }

        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(getS3Client(), extendedClientBucket);

        extendedClient = new AmazonSQSExtendedClient(getSqsClient(), extendedClientConfig);
        return extendedClient;
    }

    private S3BackedPayloadStore getPayloadStore() {
        if (payloadStore != null) {
            return payloadStore;
        }
        S3Dao s3Dao = new S3Dao(getS3Client());
        payloadStore = new S3BackedPayloadStore(s3Dao, extendedClientBucket);

        return payloadStore;
    }

    public void createQueue(String queueName) {
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(queueName)
                .build();
        getSqsClient().createQueue(createQueueRequest);
    }

    public void purgeQueue(String queueName) {
        PurgeQueueRequest purgeRequest = PurgeQueueRequest.builder()
                .queueUrl(getQueueUrl(queueName))
                .build();
        getSqsClient().purgeQueue(purgeRequest);
    }

    public void sendToQueue(String queueName, String message) {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(getQueueUrl(queueName))
                .messageBody(message)
                .build();
        getSqsClient().sendMessage(sendMessageRequest);
    }

    public List<String> readFromQueue(String queueName) {
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(getQueueUrl(queueName))
                .build();
        return getSqsClient()
                .receiveMessage(receiveMessageRequest)
                .messages()
                .stream()
                .map(Message::body)
                .collect(toList());
    }

    public void sendToExtendedQueue(String queueName, String message) {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(getQueueUrl(queueName))
                .messageBody(message)
                .build();
        getSqsExtendedClient().sendMessage(sendMessageRequest);
    }

    public void sendToExtendedQueue(String queueName, List<String> messages) {
        List<SendMessageBatchRequestEntry> sendMessageBatchRequestEntries = messages.stream()
                .map(m -> SendMessageBatchRequestEntry.builder()
                        .id(randomUUID().toString())
                        .messageBody(m)
                        .build()
                ).collect(toList());
        SendMessageBatchRequest sendMessageBatchRequest = SendMessageBatchRequest.builder()
                .queueUrl(getQueueUrl(queueName))
                .entries(sendMessageBatchRequestEntries)
                .build();
        getSqsExtendedClient().sendMessageBatch(sendMessageBatchRequest);
    }

    public List<String> readFromExtendedQueue(String queueName) {
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(getQueueUrl(queueName))
                .build();
        return getSqsExtendedClient()
                .receiveMessage(receiveMessageRequest)
                .messages()
                .stream()
                .map(Message::body)
                .collect(toList());
    }

    public String toOriginalMessage(String message) {
        return getPayloadStore().getOriginalPayload(message);
    }

    public void deleteOriginalMessage(String message) {
        getPayloadStore().deleteOriginalPayload(message);
    }

    public String storeOriginalMessage(String message) {
        return getPayloadStore().storeOriginalPayload(message);
    }

    private String getQueueUrl(String queueName) {
        GetQueueUrlRequest getQueueAttributesRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        return getSqsClient().getQueueUrl(getQueueAttributesRequest).queueUrl();
    }
}
