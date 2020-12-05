package org.vatplanner.archiver.camel;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configuration related to Camel.
 */
public class CamelConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelConfiguration.class);

    private String amqpHost;
    private int amqpPort;
    private String amqpUsername;
    private String amqpPassword;
    private String amqpVirtualHost;

    private String requestsExchange;
    private String requestsQueue;
    private Duration requestsQueueTTL;
    private int requestsConsumers;

    public String getAmqpHost() {
        return amqpHost;
    }

    public CamelConfiguration setAmqpHost(String amqpHost) {
        LOGGER.debug("setting amqpHost to {}", amqpHost);
        this.amqpHost = amqpHost;
        return this;
    }

    public int getAmqpPort() {
        return amqpPort;
    }

    public CamelConfiguration setAmqpPort(int amqpPort) {
        LOGGER.debug("setting amqpPort to {}", amqpPort);
        this.amqpPort = amqpPort;
        return this;
    }

    public String getAmqpUsername() {
        return amqpUsername;
    }

    public CamelConfiguration setAmqpUsername(String amqpUsername) {
        LOGGER.debug("setting amqpUsername to {}", amqpUsername);
        this.amqpUsername = amqpUsername;
        return this;
    }

    public String getAmqpPassword() {
        return amqpPassword;
    }

    public CamelConfiguration setAmqpPassword(String amqpPassword) {
        LOGGER.debug("setting amqpPassword (details not logged)");
        this.amqpPassword = amqpPassword;
        return this;
    }

    public String getAmqpVirtualHost() {
        return amqpVirtualHost;
    }

    public CamelConfiguration setAmqpVirtualHost(String amqpVirtualHost) {
        LOGGER.debug("setting amqpVirtualHost to {}", amqpVirtualHost);
        this.amqpVirtualHost = amqpVirtualHost;
        return this;
    }

    public String getRequestsExchange() {
        return requestsExchange;
    }

    public CamelConfiguration setRequestsExchange(String requestsExchange) {
        LOGGER.debug("setting requestsExchange to {}", requestsExchange);
        this.requestsExchange = requestsExchange;
        return this;
    }

    public String getRequestsQueue() {
        return requestsQueue;
    }

    public CamelConfiguration setRequestsQueue(String requestsQueue) {
        LOGGER.debug("setting requestsQueue to {}", requestsQueue);
        this.requestsQueue = requestsQueue;
        return this;
    }

    public Duration getRequestsQueueTTL() {
        return requestsQueueTTL;
    }

    public CamelConfiguration setRequestsQueueTTL(Duration requestsQueueTTL) {
        LOGGER.debug("setting requestsQueueTTL to {}", requestsQueueTTL);
        this.requestsQueueTTL = requestsQueueTTL;
        return this;
    }

    public int getRequestsConsumers() {
        return requestsConsumers;
    }

    public CamelConfiguration setRequestsConsumers(int requestsConsumers) {
        LOGGER.debug("setting requestsConsumers to {}", requestsConsumers);
        this.requestsConsumers = requestsConsumers;
        return this;
    }

}
