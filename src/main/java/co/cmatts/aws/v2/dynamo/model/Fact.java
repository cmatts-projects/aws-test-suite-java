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
public class Fact implements DynamoDbMappedBean {
    @Getter(onMethod_ = @DynamoDbPartitionKey)
    private Integer id;
    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "personIndex"))
    private Integer personId;
    private Integer year;
    private String image;
    private String source;
    private String description;
    @Getter(onMethod_ = @DynamoDbVersionAttribute)
    private Long version;

    @Override
    public String tableName() {
        return "facts";
    }
}
