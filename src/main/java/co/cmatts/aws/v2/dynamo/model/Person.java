package co.cmatts.aws.v2.dynamo.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Data
@Builder
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Person implements DynamoDbMappedBean {
    @Getter(onMethod_ = @DynamoDbPartitionKey)
    private Integer id;
    private String name;
    private Integer yearOfBirth;
    private Integer yearOfDeath;
    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "fatherIndex"))
    private Integer fatherId;
    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "motherIndex"))
    private Integer motherId;
    @Getter(onMethod_ = @DynamoDbVersionAttribute)
    private Long version;

    public String tableName() {
        return "people";
    }
}
