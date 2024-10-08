package co.cmatts.aws.v1.sqs;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.payloadoffloading.S3BackedPayloadStore;
import software.amazon.payloadoffloading.S3Dao;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static co.cmatts.aws.v1.client.Configuration.configureEndPoint;
import static co.cmatts.aws.v1.s3.S3.getS3Client;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

public class Sqs {
    private static final int MAX_MESSAGE_BYTES = 256000;
    private static final int MAX_BATCH_SIZE = 10;

    private final String extendedClientBucket;

    private S3BackedPayloadStore payloadStore;

    private AmazonSQS client;
    private AmazonSQS extendedClient;

    public Sqs() {
        extendedClientBucket = null;
    }

    public Sqs(String extendedClientBucket) {
        this.extendedClientBucket = extendedClientBucket;
    }

    private AmazonSQS getSqsClient() {
        if (client != null) {
            return client;
        }

        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
        configureEndPoint(builder);

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

        ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(getS3Client(), extendedClientBucket);

        extendedClient = new AmazonSQSExtendedClient(getSqsClient(), extendedClientConfig);
        ;
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
        String queueUrl = getQueueUrl(queueName);
        List<List<SendMessageBatchRequestEntry>> batches = splitBatchRequests(messages);
        batches.forEach(batchEntries -> {
            getSqsExtendedClient().sendMessageBatch(queueUrl, batchEntries);
        });
    }

    private List<List<SendMessageBatchRequestEntry>> splitBatchRequests(List<String> messages) {
        List<List<SendMessageBatchRequestEntry>> batches = new ArrayList<>();
        List<SendMessageBatchRequestEntry> batchEntries = new ArrayList<>();
        int currentBatchByteSize = 0;
        for (String m : messages) {
            SendMessageBatchRequestEntry batchRequest = new SendMessageBatchRequestEntry(randomUUID().toString(), m);
            int batchRequestByteSize = batchRequest.toString().getBytes(StandardCharsets.UTF_8).length;
            if (MAX_MESSAGE_BYTES < (currentBatchByteSize + batchRequestByteSize) ||
                    MAX_BATCH_SIZE <= batchEntries.size()) {
                batches.add(batchEntries);
                batchEntries = new ArrayList<>();
                currentBatchByteSize = 0;
            }
            currentBatchByteSize += batchRequestByteSize;
            batchEntries.add(batchRequest);
        }
        batches.add(batchEntries);
        return batches;
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
        return getPayloadStore().storeOriginalPayload(message, (long) message.getBytes().length);
    }

    private String getQueueUrl(String queueName) {
        return getSqsClient().getQueueUrl(queueName).getQueueUrl();
    }
}
