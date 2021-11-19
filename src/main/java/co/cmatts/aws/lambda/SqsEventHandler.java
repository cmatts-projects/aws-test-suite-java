package co.cmatts.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import co.cmatts.aws.sqs.SqsClient;

public class SqsEventHandler implements RequestHandler<SQSEvent, Void> {

    private SqsClient sqsClient = new SqsClient();
    private String queueName = System.getenv("FORWARD_QUEUE");

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        event.getRecords().stream()
                .forEach(this::doSomething);

        return null;
    }

    private void doSomething(SQSMessage sqsMessage) {
        sqsClient.sendToQueue(queueName, sqsMessage.getBody());
    }

}
