package dynamo.model;

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
@DynamoDBTable(tableName = "people")
public class Person {
    @DynamoDBHashKey
    private Integer id;
    private String name;
    private Integer yearOfBirth;
    private Integer yearOfDeath;
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "fatherIndex")
    private Integer fatherId;
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "motherIndex")
    private Integer motherId;
    @DynamoDBVersionAttribute
    private Long version;
}
