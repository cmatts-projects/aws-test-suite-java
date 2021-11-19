package co.cmatts.aws.cloudformation;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;

import java.io.IOException;

public class DynamoStackRequestFactory {
    private static final String DYNAMO_TABLES_YML = "dynamo-tables.yml";

    public static CreateStackRequest createDynamoDbStackRequest() throws IOException {
        String contents = new String(DynamoStackRequestFactory.class.getClassLoader()
                .getResourceAsStream(DYNAMO_TABLES_YML).readAllBytes());

        return new CreateStackRequest()
                .withStackName("DynamoDB")
                .withTemplateBody(contents)
                .withResourceTypes("AWS::DynamoDB::Table");
    }

}
