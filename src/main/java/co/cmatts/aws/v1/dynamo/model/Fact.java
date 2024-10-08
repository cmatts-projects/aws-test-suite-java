package co.cmatts.aws.v1.dynamo.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import lombok.*;

@Data
@Builder
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "facts")
public class Fact {
    @DynamoDBHashKey
    private Integer id;
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "personIndex")
    private Integer personId;
    private Integer year;
    private String image;
    private String source;
    private String description;
    @DynamoDBVersionAttribute
    private Long version;
}
