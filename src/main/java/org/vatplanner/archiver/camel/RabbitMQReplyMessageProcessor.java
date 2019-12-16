package org.vatplanner.archiver.camel;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

/**
 * Camel Processor to turn a message received by a RabbitMQ endpoint into a
 * reply message for replying via a RabbitMQ endpoint. Use
 * RabbitMQReplyMsg.getInstance() to get a singleton instance, processor does
 * not require any state, so one central instance is sufficient.
 */
public class RabbitMQReplyMessageProcessor implements Processor {

    private static RabbitMQReplyMessageProcessor instance = null;

    /**
     * Returns a single shared instance of this processor.
     *
     * @return shared instance
     */
    public static RabbitMQReplyMessageProcessor getInstance() {
        synchronized (RabbitMQReplyMessageProcessor.class) {
            if (instance == null) {
                instance = new RabbitMQReplyMessageProcessor();
            }
        }

        return instance;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        Message out = in.copy();
        Map<String, Object> headersIn = in.getHeaders();

        // remove headers unsuitable for replies
        out.removeHeader("rabbitmq.REPLY_TO");
        out.removeHeader("rabbitmq.EXPIRATION");

        // force default exchange, workaround for bug CAMEL-8270
        //out.setHeader("rabbitmq.EXCHANGE_NAME", "");
        // default exchange routes messages to queue = routing_key
        String replyQueueName = (String) headersIn.getOrDefault("rabbitmq.REPLY_TO", "amq.rabbitmq.reply-to");
        out.setHeader("rabbitmq.ROUTING_KEY", replyQueueName);

        exchange.setMessage(out);
    }
}
