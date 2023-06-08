package co.cmatts.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import co.cmatts.aws.v1.sqs.Sqs;

public class LargeSqsEventHandler implements RequestHandler<SQSEvent, Void> {

    private Sqs sqs = new Sqs(System.getenv("EXTENDED_CLIENT_BUCKET"));
    private String queueName = System.getenv("FORWARD_QUEUE");

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        event.getRecords().stream()
                .forEach(this::doSomething);

        return null;
    }

    private void doSomething(SQSMessage sqsMessage) {
        String originalMessage = sqs.toOriginalMessage(sqsMessage.getBody());
        sqs.sendToExtendedQueue(queueName, originalMessage);
        sqs.deleteOriginalMessage(sqsMessage.getBody());
    }
}
