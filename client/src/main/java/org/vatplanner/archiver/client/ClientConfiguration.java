package org.vatplanner.archiver.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConfiguration.class);

    private String amqpHost;
    private int amqpPort;
    private String amqpUsername;
    private String amqpPassword;
    private String amqpVirtualHost;

    private String requestsExchange;

    public String getAmqpHost() {
        return amqpHost;
    }

    public ClientConfiguration setAmqpHost(String amqpHost) {
        LOGGER.debug("setting amqpHost to {}", amqpHost);
        this.amqpHost = amqpHost;
        return this;
    }

    public int getAmqpPort() {
        return amqpPort;
    }

    public ClientConfiguration setAmqpPort(int amqpPort) {
        LOGGER.debug("setting amqpPort to {}", amqpPort);
        this.amqpPort = amqpPort;
        return this;
    }

    public String getAmqpUsername() {
        return amqpUsername;
    }

    public ClientConfiguration setAmqpUsername(String amqpUsername) {
        LOGGER.debug("setting amqpUsername to {}", amqpUsername);
        this.amqpUsername = amqpUsername;
        return this;
    }

    public String getAmqpPassword() {
        return amqpPassword;
    }

    public ClientConfiguration setAmqpPassword(String amqpPassword) {
        LOGGER.debug("setting amqpPassword (details not logged)");
        this.amqpPassword = amqpPassword;
        return this;
    }

    public String getAmqpVirtualHost() {
        return amqpVirtualHost;
    }

    public ClientConfiguration setAmqpVirtualHost(String amqpVirtualHost) {
        LOGGER.debug("setting amqpVirtualHost to {}", amqpVirtualHost);
        this.amqpVirtualHost = amqpVirtualHost;
        return this;
    }

    public String getRequestsExchange() {
        return requestsExchange;
    }

    public ClientConfiguration setRequestsExchange(String requestsExchange) {
        LOGGER.debug("setting requestsExchange to {}", requestsExchange);
        this.requestsExchange = requestsExchange;
        return this;
    }

}
