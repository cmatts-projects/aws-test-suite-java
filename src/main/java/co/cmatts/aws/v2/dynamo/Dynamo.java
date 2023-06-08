package co.cmatts.aws.v2.dynamo;

import co.cmatts.aws.v2.dynamo.model.DynamoDbMappedBean;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.util.HashMap;
import java.util.Map;

import static co.cmatts.aws.v2.client.Configuration.configureEndPoint;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

public class Dynamo {

    public static final String TABLE_NAME_PREFIX = "dynamo.example.";
    private static final Map<String, DynamoDbAsyncTable<?  extends DynamoDbMappedBean>> tables = new HashMap<>();
    private static DynamoDbEnhancedAsyncClient enhancedClient;
    private static DynamoDbEnhancedAsyncClient enhancedUnversionedClient;

    public static DynamoDbEnhancedAsyncClient getEnhancedDynamoClient() {
        if (!isNull(enhancedClient)) {
            return enhancedClient;
        }

        DynamoDbEnhancedAsyncClient.Builder builder = DynamoDbEnhancedAsyncClient.builder();
        builder.dynamoDbClient((DynamoDbAsyncClient) configureEndPoint(DynamoDbAsyncClient.builder()).build());

        enhancedClient = builder.build();
        return enhancedClient;
    }

    public static DynamoDbEnhancedAsyncClient getEnhancedUnversionedDynamoClient() {
        if (!isNull(enhancedUnversionedClient)) {
            return enhancedUnversionedClient;
        }

        DynamoDbEnhancedAsyncClient.Builder builder = DynamoDbEnhancedAsyncClient.builder();
        builder.dynamoDbClient((DynamoDbAsyncClient) configureEndPoint(DynamoDbAsyncClient.builder())
                .build())
                .extensions(emptyList());

        enhancedUnversionedClient = builder.build();

        return enhancedUnversionedClient;
    }

    public static <T extends DynamoDbMappedBean> DynamoDbAsyncTable<T> getDynamoTable(Class<T> beanClass) {
        DynamoDbAsyncTable<T> table = (DynamoDbAsyncTable<T>)tables.get(beanClass.getSimpleName());
        if (isNull(table)) {
            table = getDynamoTable(getEnhancedDynamoClient(), beanClass);
            tables.put(beanClass.getSimpleName(), table);
        }
        return table;
    }

    public static <T extends DynamoDbMappedBean> DynamoDbAsyncTable<T> getDynamoTable(DynamoDbEnhancedAsyncClient client, Class<T> beanClass) {
        return client.table(getTableName(beanClass), TableSchema.fromBean(beanClass));
    }

    private static String getTableName(Class<? extends DynamoDbMappedBean> beanClass) {
        String tableName;
        try {
            DynamoDbMappedBean instance = beanClass.getDeclaredConstructor().newInstance();
            tableName = TABLE_NAME_PREFIX + instance.tableName();
        } catch (Exception e) {
            throw new IllegalArgumentException("getDynamoTable expects a DynamoDbMappedBean");
        }
        return tableName;
    }

}
