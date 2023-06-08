package co.cmatts.aws.v1.cloudwatch;

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

import static co.cmatts.aws.v1.client.Configuration.configureEndPoint;
import static com.amazonaws.services.cloudwatch.model.Statistic.Average;
import static java.time.ZoneOffset.UTC;

public class CloudWatch {

    private static AmazonCloudWatch client;

    private static AmazonCloudWatch getCloudWatchClient() {
        if (client != null) {
            return client;
        }

        AmazonCloudWatchClientBuilder builder = AmazonCloudWatchClientBuilder.standard();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public static MetricDatum createMetric(String dimensionName, String dimensionValue, String metricName, int value) {
        Dimension dimension = new Dimension()
                .withName(dimensionName)
                .withValue(dimensionValue);
        return new MetricDatum()
                .withMetricName(metricName)
                .withValue((double)value)
                .withUnit(StandardUnit.Count)
                .withDimensions(dimension);
    }

    public static List<PutMetricDataResult> logMetrics(List<MetricDatum> metrics, String namespace) {
        List<PutMetricDataResult> response = new ArrayList<>();
        ListUtils.partition(metrics, 25)
                .forEach(metricsBatch -> {
                    PutMetricDataRequest request = new PutMetricDataRequest()
                            .withMetricData(metricsBatch)
                            .withNamespace(namespace);
                    response.add(getCloudWatchClient().putMetricData(request));
                });
        return response;
    }

    public static double getAverageForDays(int days, String dimensionName, String dimensionValue, String namespace, String metricName) {
        LocalDateTime now = LocalDateTime.now();
        Date startTime = Date.from(now.minusDays(days).toInstant(UTC));
        Date endTime = Date.from(now.toInstant(UTC));

        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                .withStartTime(startTime)
                .withEndTime(endTime)
                .withPeriod((int)TimeUnit.DAYS.toSeconds(days))
                .withStatistics(Average)
                .withNamespace(namespace)
                .withMetricName(metricName)
                .withDimensions(new Dimension().withName(dimensionName).withValue(dimensionValue));

        GetMetricStatisticsResult response = getCloudWatchClient().getMetricStatistics(request);

        Optional<Datapoint> datapoint = response.getDatapoints().stream().findFirst();

        return datapoint.map(Datapoint::getAverage).orElse(0d);
    }
}
