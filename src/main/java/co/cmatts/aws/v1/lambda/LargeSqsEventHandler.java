package co.cmatts.aws.v1.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import co.cmatts.aws.v1.sqs.SqsClient;

public class LargeSqsEventHandler implements RequestHandler<SQSEvent, Void> {

    private SqsClient sqsClient = new SqsClient(System.getenv("EXTENDED_CLIENT_BUCKET"));
    private String queueName = System.getenv("FORWARD_QUEUE");

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        event.getRecords().stream()
                .forEach(this::doSomething);

        return null;
    }

    private void doSomething(SQSMessage sqsMessage) {
        String originalMessage = sqsClient.toOriginalMessage(sqsMessage.getBody());
        sqsClient.sendToExtendedQueue(queueName, originalMessage);
        sqsClient.deleteOriginalMessage(sqsMessage.getBody());
    }
}
