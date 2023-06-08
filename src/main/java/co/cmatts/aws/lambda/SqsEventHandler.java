package co.cmatts.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import co.cmatts.aws.v1.sqs.Sqs;

public class SqsEventHandler implements RequestHandler<SQSEvent, Void> {

    private Sqs sqs = new Sqs();
    private String queueName = System.getenv("FORWARD_QUEUE");

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        event.getRecords().stream()
                .forEach(this::doSomething);

        return null;
    }

    private void doSomething(SQSMessage sqsMessage) {
        sqs.sendToQueue(queueName, sqsMessage.getBody());
    }

}
