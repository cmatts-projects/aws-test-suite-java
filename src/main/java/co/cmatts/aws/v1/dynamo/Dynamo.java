package co.cmatts.aws.v1.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;

import static co.cmatts.aws.v1.client.Configuration.configureEndPoint;

public class Dynamo {

    public static final String TABLE_NAME_PREFIX = "dynamo.example.";
    private static AmazonDynamoDB client;
    private static DynamoDBMapper mapper;

    public static AmazonDynamoDB getDynamoClient() {
        if (client != null) {
            return client;
        }

        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public static DynamoDBMapperConfig getDynamoMapperConfig() {
        return DynamoDBMapperConfig.builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNamePrefix(TABLE_NAME_PREFIX))
                .build();
    }

    public static DynamoDBMapper getDynamoMapper() {
        if (mapper != null) {
            return mapper;
        }
        mapper = new DynamoDBMapper(getDynamoClient());
        return mapper;
    }

}
