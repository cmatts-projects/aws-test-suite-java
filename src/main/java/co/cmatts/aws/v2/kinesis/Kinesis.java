package co.cmatts.aws.v2.kinesis;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static co.cmatts.aws.v2.client.Configuration.configureEndPoint;
import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.awssdk.services.kinesis.model.ShardIteratorType.TRIM_HORIZON;


public class Kinesis {
    private static final String MY_KEY = "myKey";

    private static KinesisClient client;
    private List<Record> receivedRecords = new ArrayList<>();
    private Timer timer;
    private String streamName;


    private KinesisClient getKinesisClient() {
        if (client != null) {
            return client;
        }

        KinesisClientBuilder builder = KinesisClient.builder();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public void createStream(String name, int numberOfShards) {
        streamName = name;
        CreateStreamRequest createStreamRequest = CreateStreamRequest.builder()
                .streamName(streamName)
                .shardCount(numberOfShards)
                .build();
        getKinesisClient().createStream(createStreamRequest);
        waitForKinesisToBeActive();
    }

    private void waitForKinesisToBeActive() {
        DescribeStreamRequest describeStreamRequest = DescribeStreamRequest
                .builder()
                .streamName(streamName)
                .build();

        getKinesisClient().waiter().waitUntilStreamExists(describeStreamRequest);
    }

    public StreamStatus getStreamStatus() {
        DescribeStreamRequest describeStreamRequest = DescribeStreamRequest
                .builder()
                .streamName(streamName)
                .build();

        return getKinesisClient()
                .describeStream(describeStreamRequest)
                .streamDescription()
                .streamStatus();
    }

    public void sendToKinesis(List<String> messages) {
        List<PutRecordsRequestEntry> putRecords = messages.stream()
                .map(message -> PutRecordsRequestEntry.builder()
                        .partitionKey(MY_KEY)
                        .data(stringToSdkBytes(message))
                        .build())
                .collect(Collectors.toList());

        PutRecordsRequest request = PutRecordsRequest.builder()
                .streamName(streamName)
                .records(putRecords)
                .build();

        getKinesisClient().putRecords(request);
    }

    private SdkBytes stringToSdkBytes(String message) {
        return SdkBytes.fromString(message, UTF_8);
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

    public List<Record> getReceivedRecords() {
        ArrayList<Record> records = new ArrayList<>(receivedRecords);
        if (!records.isEmpty()) {
            receivedRecords = receivedRecords.subList(records.size() - 1, receivedRecords.size() - 1);
        }
        return records;
    }

    private void readStream() {
        ListShardsRequest listShardsRequest = ListShardsRequest
                .builder()
                .streamName(streamName)
                .build();
        ListShardsResponse shards = getKinesisClient().listShards(listShardsRequest);

        shards.shards().forEach(shard -> receivedRecords.addAll(readStream(shard)));
    }

    private List<Record> readStream(Shard shard) {
        GetShardIteratorRequest getShardIteratorRequest = GetShardIteratorRequest
                .builder()
                .streamName(streamName)
                .shardId(shard.shardId())
                .shardIteratorType(TRIM_HORIZON)
                .build();

        GetShardIteratorResponse getShardIteratorResponse = getKinesisClient()
                .getShardIterator(getShardIteratorRequest);
        String shardIterator = getShardIteratorResponse.shardIterator();

        GetRecordsRequest getRecordsRequest = GetRecordsRequest
                .builder()
                .shardIterator(shardIterator)
                .limit(25)
                .build();

        GetRecordsResponse getRecordsResponse = getKinesisClient().getRecords(getRecordsRequest);
        return getRecordsResponse.records();
    }
}
