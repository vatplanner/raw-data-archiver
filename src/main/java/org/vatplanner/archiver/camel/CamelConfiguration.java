package org.vatplanner.archiver.camel;

import java.time.Duration;

/**
 * Holds configuration related to Camel.
 */
public class CamelConfiguration {

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
        this.amqpHost = amqpHost;
        return this;
    }

    public int getAmqpPort() {
        return amqpPort;
    }

    public CamelConfiguration setAmqpPort(int amqpPort) {
        this.amqpPort = amqpPort;
        return this;
    }

    public String getAmqpUsername() {
        return amqpUsername;
    }

    public CamelConfiguration setAmqpUsername(String amqpUsername) {
        this.amqpUsername = amqpUsername;
        return this;
    }

    public String getAmqpPassword() {
        return amqpPassword;
    }

    public CamelConfiguration setAmqpPassword(String amqpPassword) {
        this.amqpPassword = amqpPassword;
        return this;
    }

    public String getAmqpVirtualHost() {
        return amqpVirtualHost;
    }

    public CamelConfiguration setAmqpVirtualHost(String amqpVirtualHost) {
        this.amqpVirtualHost = amqpVirtualHost;
        return this;
    }

    public String getRequestsExchange() {
        return requestsExchange;
    }

    public CamelConfiguration setRequestsExchange(String requestsExchange) {
        this.requestsExchange = requestsExchange;
        return this;
    }

    public String getRequestsQueue() {
        return requestsQueue;
    }

    public CamelConfiguration setRequestsQueue(String requestsQueue) {
        this.requestsQueue = requestsQueue;
        return this;
    }

    public Duration getRequestsQueueTTL() {
        return requestsQueueTTL;
    }

    public CamelConfiguration setRequestsQueueTTL(Duration requestsQueueTTL) {
        this.requestsQueueTTL = requestsQueueTTL;
        return this;
    }

    public int getRequestsConsumers() {
        return requestsConsumers;
    }

    public CamelConfiguration setRequestsConsumers(int requestsConsumers) {
        this.requestsConsumers = requestsConsumers;
        return this;
    }

}
