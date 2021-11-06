package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import lambda.model.MyEvent;

public class SimpleEventHandler implements RequestHandler<MyEvent, String> {

    @Override
    public String handleRequest(MyEvent event, Context context) {
        return event.getMessage();
    }

}
