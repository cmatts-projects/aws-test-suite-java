package co.cmatts.aws.kinesis;

import co.cmatts.aws.client.Configuration;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.kinesis.model.*;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.CallExecutorBuilder;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static co.cmatts.aws.client.Configuration.configureEndPoint;
import static com.amazonaws.services.kinesis.model.ShardIteratorType.TRIM_HORIZON;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;


public class KinesisClient {
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile("http://(.*?):(\\d+)");
    private static final String MY_KEY = "myKey";

    private static AmazonKinesis client;
    private static KinesisProducer producer;
    private List<Record> receivedRecords = new ArrayList<>();
    private Timer timer;
    private String streamName;


    private AmazonKinesis getKenisisClient() {
        if (client != null) {
            return client;
        }

        AmazonKinesisClientBuilder builder = AmazonKinesisClientBuilder.standard();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public void createStream(String name, int numberOfShards) {
        streamName = name;
        getKenisisClient().createStream(name, numberOfShards);
        waitForKinesisToBeActive();
    }

    private void waitForKinesisToBeActive() {
        try {
            RetryConfig retryConfig = new RetryConfigBuilder().withMaxNumberOfTries(30)
                    .withFixedBackoff().withDelayBetweenTries(1, SECONDS)
                    .retryOnSpecificExceptions(RuntimeException.class)
                    .retryOnReturnValue("CREATING")
                    .build();
            CallExecutor builder = new CallExecutorBuilder()
                    .config(retryConfig)
                    .build();
            builder.execute(() -> getStreamStatus());
        } catch (Exception any) {
            throw new IllegalStateException("Kinesis not started.", any);
        }
    }

    public String getStreamStatus() {
        return getKenisisClient()
                .describeStream(streamName)
                .getStreamDescription()
                .getStreamStatus();
    }

    /**
     * KPL adds value to the Kinesis client. It aggregates puts and sends events as batches to optimise throughput
     */
    private KinesisProducer getProducer() {
        if (producer != null) {
            return producer;
        }

        KinesisProducerConfiguration config = new KinesisProducerConfiguration();
        String localKinesisEndpoint = System.getenv("LOCAL_STACK_ENDPOINT");
        String awsRegion = System.getenv("AWS_REGION");

        if (localKinesisEndpoint != null && awsRegion != null) {
            Matcher m = ENDPOINT_PATTERN.matcher(localKinesisEndpoint);
            if (m.matches()) {
                config.setKinesisEndpoint(m.group(1));
                config.setKinesisPort(Integer.parseInt(m.group(2)));
                config.setRegion(awsRegion);
                config.setVerifyCertificate(false);
                config.setAggregationEnabled(true);
            }
        }
        producer = new KinesisProducer(config);
        return producer;
    }

    public void sendToKinesis(List<String> messages) {
        List<Future<UserRecordResult>> putFutures = messages.stream()
                .map(message -> getProducer().addUserRecord(streamName, MY_KEY, stringToByteBuffer(message)))
                .collect(Collectors.toList());

        // Wait for puts to finish and check the results
        putFutures.forEach(this::waitForDespatch);
    }

    private void waitForDespatch(Future<UserRecordResult> recordResultFuture) {
        UserRecordResult result = null;
        try {
            result = recordResultFuture.get();
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected exception sending to Kinesis", e);
        }

        if (!result.isSuccessful()) {
            throw new IllegalStateException("Expected message to be sent to Kinesis");
        }
    }

    private ByteBuffer stringToByteBuffer(String message) {
        return ByteBuffer.wrap(message.getBytes(UTF_8));
    }

    public void startKinesisListener() {
        TimerTask task = new TimerTask() {
            public void run() {
                readStream();
            }
        };
        timer = new Timer("Kinesis Listener");
        long period = 1000L;
        timer.schedule(task, 0, period);
    }

    public void stopKinesisListener() {
        timer.cancel();
    }

    public  List<Record> getReceivedRecords() {
        ArrayList<Record> records = new ArrayList<>(receivedRecords);
        if (!records.isEmpty()) {
            receivedRecords = receivedRecords.subList(records.size() - 1, receivedRecords.size() - 1);
        }
        return records;
    }

    private void readStream() {
        ListShardsRequest listShardsRequest = new ListShardsRequest().withStreamName(streamName);
        ListShardsResult shards = getKenisisClient().listShards(listShardsRequest);

        shards.getShards().forEach(shard -> {
            receivedRecords.addAll(readStream(shard));
        });
    }

    private List<Record> readStream(Shard shard) {
        GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest();
        getShardIteratorRequest.setStreamName(streamName);
        getShardIteratorRequest.setShardId(shard.getShardId());
        getShardIteratorRequest.setShardIteratorType(TRIM_HORIZON);

        GetShardIteratorResult getShardIteratorResult = client.getShardIterator(getShardIteratorRequest);
        String shardIterator = getShardIteratorResult.getShardIterator();

        GetRecordsRequest getRecordsRequest = new GetRecordsRequest();
        getRecordsRequest.setShardIterator(shardIterator);
        getRecordsRequest.setLimit(25);

        GetRecordsResult getRecordsResult = getKenisisClient().getRecords(getRecordsRequest);
        return getRecordsResult.getRecords();
    }
}
