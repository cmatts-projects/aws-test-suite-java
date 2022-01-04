package co.cmatts.aws.cloudwatch;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.*;
import org.apache.commons.collections4.ListUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.cloudwatch.model.Statistic.Average;
import static java.time.ZoneOffset.UTC;

public class CloudWatchClient {

    private static final String MY_DIMENSION = "myDimension";
    private static final String MY_COUNT = "myCount";
    private static final String MY_NAMESPACE = "myNamespace";

    private AmazonCloudWatch client;

    private AmazonCloudWatch getCloudWatchClient() {
        if (client != null) {
            return client;
        }

        AmazonCloudWatchClientBuilder builder = AmazonCloudWatchClientBuilder.standard();
        String localCloudWatchEndpoint = System.getenv("LOCAL_STACK_ENDPOINT");
        String awsRegion = System.getenv("AWS_REGION");

        if (localCloudWatchEndpoint != null && awsRegion != null) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(localCloudWatchEndpoint, awsRegion));
        }

        client = builder.build();
        return client;
    }

    public MetricDatum createMetric(String metricName, int value) {
        Dimension dimension = new Dimension()
                .withName(MY_DIMENSION)
                .withValue(MY_COUNT);
        return new MetricDatum()
                .withMetricName(metricName)
                .withValue((double)value)
                .withUnit(StandardUnit.Count)
                .withDimensions(dimension);
    }

    public List<PutMetricDataResult> logMetrics(List<MetricDatum> metrics) {
        List<PutMetricDataResult> response = new ArrayList<>();
        ListUtils.partition(metrics, 25)
                .forEach(metricsBatch -> {
                    PutMetricDataRequest request = new PutMetricDataRequest()
                            .withMetricData(metricsBatch)
                            .withNamespace(MY_NAMESPACE);
                    response.add(getCloudWatchClient().putMetricData(request));
                });
        return response;
    }

    public double getAverageForDays(String metricName, int days) {
        LocalDateTime now = LocalDateTime.now();
        Date startTime = Date.from(now.minusDays(days).toInstant(UTC));
        Date endTime = Date.from(now.toInstant(UTC));

        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withPeriod((int)TimeUnit.DAYS.toSeconds(days))
                .withStatistics(Average)
                .withNamespace(MY_NAMESPACE)
                .withMetricName(metricName)
                .withDimensions(new Dimension().withName(MY_DIMENSION).withValue(MY_COUNT));

        GetMetricStatisticsResult response = getCloudWatchClient().getMetricStatistics(request);

        Optional<Datapoint> datapoint = response.getDatapoints().stream().findFirst();

        return datapoint.map(Datapoint::getAverage).orElse(0d);
    }
}
