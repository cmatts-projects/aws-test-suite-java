package co.cmatts.aws.v2.cloudwatch;

import org.apache.commons.collections4.ListUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClientBuilder;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static co.cmatts.aws.v2.client.Configuration.configureEndPoint;
import static java.time.ZoneOffset.UTC;

public class CloudWatch {

    private static CloudWatchClient client;

    private static CloudWatchClient getCloudWatchClient() {
        if (client != null) {
            return client;
        }

        CloudWatchClientBuilder builder = CloudWatchClient.builder();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public static MetricDatum createMetric(String dimensionName, String dimensionValue, String metricName, int value, Instant timestamp) {
        Dimension dimension = Dimension.builder()
                .name(dimensionName)
                .value(dimensionValue)
                .build();

        return MetricDatum.builder()
                .metricName(metricName)
                .value((double) value)
                .unit(StandardUnit.COUNT)
                .dimensions(dimension)
                .timestamp(timestamp)
                .build();
    }

    public static List<PutMetricDataResponse> logMetrics(List<MetricDatum> metrics, String namespace) {
        List<PutMetricDataResponse> response = new ArrayList<>();
        ListUtils.partition(metrics, 25)
                .forEach(metricsBatch -> {
                    PutMetricDataRequest request = PutMetricDataRequest.builder()
                            .namespace(namespace)
                            .metricData(metricsBatch).build();

                    response.add(getCloudWatchClient().putMetricData(request));
                });
        return response;
    }

    public static double getAverageForDays(int days, String dimensionName, String dimensionValue, String namespace, String metricName) {
        LocalDateTime now = LocalDateTime.now();
        Instant startTime = now.minusDays(days).toInstant(UTC);
        Instant endTime = now.toInstant(UTC);

        GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                .startTime(startTime)
                .endTime(endTime)
                .period((int) TimeUnit.DAYS.toSeconds(days))
                .statistics(Statistic.AVERAGE)
                .namespace(namespace)
                .metricName(metricName)
                .dimensions(Dimension.builder().name(dimensionName).value(dimensionValue).build())
                .build();

        GetMetricStatisticsResponse response = getCloudWatchClient().getMetricStatistics(request);

        Optional<Datapoint> datapoint = response.datapoints().stream().findFirst();

        return datapoint.map(Datapoint::average).orElse(0d);
    }
}
