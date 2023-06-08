package co.cmatts.aws.v2.dynamo.model;

public interface DynamoDbMappedBean {
    default String tableName() {
        return Runtime.class.getClass().getSimpleName().toLowerCase();
    }

    Long getVersion();

    void setVersion(Long version);
}
