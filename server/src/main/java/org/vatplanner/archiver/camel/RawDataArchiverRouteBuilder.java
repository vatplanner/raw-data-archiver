package org.vatplanner.archiver.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rabbitmq.RabbitMQEndpoint;
import org.vatplanner.archiver.local.Loader;
import org.vatplanner.archiver.remote.PackerFactory;

/**
 * Configures all message routes for Camel.
 */
public class RawDataArchiverRouteBuilder extends RouteBuilder {

    private final Loader loader;
    private final PackerFactory packerFactory;
    private final CamelConfiguration config;
    private final CamelContext context;

    public RawDataArchiverRouteBuilder(CamelContext context, CamelConfiguration config, Loader loader,
        PackerFactory packerFactory) {
        super(context);

        this.context = context;
        this.config = config;
        this.loader = loader;
        this.packerFactory = packerFactory;
    }

    private void configureCommonSettings(RabbitMQEndpoint endpoint) {
        endpoint.setUsername(config.getAmqpUsername());
        endpoint.setPassword(config.getAmqpPassword());
        endpoint.setVhost(config.getAmqpVirtualHost());
        endpoint.setAutomaticRecoveryEnabled(true);
        endpoint.setConnectionTimeout(30000);
        endpoint.setRequestedHeartbeat(10);

        // configure prefetcher as workaround for CAMEL-8308 to avoid locking
        // all messages on server (which prevents message expiration)
        // see: https://issues.apache.org/jira/browse/CAMEL-8308
        endpoint.setPrefetchEnabled(true);
        endpoint.setPrefetchCount(1);
    }

    @Override
    public void configure() throws Exception {
        String baseURL = "rabbitmq://" + config.getAmqpHost() + ":" + config.getAmqpPort() + "/";

        // define common endpoint for sending direct replies
        RabbitMQEndpoint amqpOutDirect = (RabbitMQEndpoint) context.getEndpoint(baseURL + "amq.direct");
        configureCommonSettings(amqpOutDirect);
        amqpOutDirect.setDeclare(false);
        amqpOutDirect.setExchangeType("direct");
        amqpOutDirect.setExchangePattern(ExchangePattern.InOnly);

        // incoming requests
        RabbitMQEndpoint amqpInRequests = (RabbitMQEndpoint) context.getEndpoint(
            baseURL + config.getRequestsExchange() //
        );
        configureCommonSettings(amqpInRequests);
        amqpInRequests.setExchangeType("direct");
        amqpInRequests.setConcurrentConsumers(config.getRequestsConsumers());
        amqpInRequests.setDeclare(true);
        amqpInRequests.setAutoAck(true); // if we crash, processing should NOT be retried as it would likely be
                                         // reproduceable and cause DoS
        amqpInRequests.setAutoDelete(false);
        amqpInRequests.setQueue(config.getRequestsQueue());
        amqpInRequests.getArgs().put("arg.queue.x-message-ttl", config.getRequestsQueueTTL().toMillis()); // TODO: test

        DataFileRequestProcessor dataFileRequestProcessor = new DataFileRequestProcessor(loader, packerFactory);
        from(amqpInRequests)
            .process(dataFileRequestProcessor)
            .process(RabbitMQReplyMessageProcessor.getInstance())
            .to(ExchangePattern.InOnly, amqpOutDirect);

        // TODO: test response to RPC
    }

}
