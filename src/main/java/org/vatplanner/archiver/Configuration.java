package org.vatplanner.archiver;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Properties;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.camel.CamelConfiguration;
import org.vatplanner.archiver.local.StorageConfiguration;
import org.vatplanner.archiver.remote.PackerConfiguration;

/**
 * Loads all configuration for the application.
 */
public class Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final StorageConfiguration storageConfig;
    private final PackerConfiguration packerConfig;
    private final CamelConfiguration camelConfig;

    private static final String LOCAL_PROPERTIES_DIRECTORY_NAME = ".vatplanner";
    private static final String LOCAL_PROPERTIES_FILE_NAME = "raw-data-archiver.properties";
    private static final String DEFAULT_PROPERTIES_RESOURCE = LOCAL_PROPERTIES_FILE_NAME;

    /**
     * Loads all application configuration.
     *
     * @param filePath optional path to local configuration file; null to load
     * local configuration from default path
     */
    public Configuration(String filePath) {
        boolean success = true;

        Properties properties = new Properties();
        success &= load(properties, getDefaultConfigurationUrl());
        success &= load(properties, getLocalConfigurationFile(filePath));

        if (!success) {
            throw new RuntimeException("Failed to read configuration");
        }

        storageConfig = parseStorageConfiguration(properties);
        packerConfig = parsePackerConfiguration(properties);
        camelConfig = parseCamelConfiguration(properties);
    }

    /**
     * Overloads configuration options from given file into {@link Properties}.
     *
     * @param properties properties to receive overloaded options
     * @param file configuration file to read
     * @return true if file was read successfully, false if an error occurred
     */
    private boolean load(Properties properties, File file) {
        LOGGER.info("Loading configuration from file {}", file);

        try (FileReader fr = new FileReader(file)) {
            properties.load(fr);
            return true;
        } catch (IOException ex) {
            LOGGER.error("Failed to read configuration from file " + file, ex);
            return false;
        }
    }

    /**
     * Overloads configuration options from given {@link URL} into
     * {@link Properties}.
     *
     * @param properties properties to receive overloaded options
     * @param url URL of configuration file to read
     * @return true if file was read successfully, false if an error occurred
     */
    private boolean load(Properties properties, URL url) {
        LOGGER.info("Loading configuration from URL {}", url);
        try (InputStream is = url.openStream()) {
            properties.load(is);
            return true;
        } catch (IOException ex) {
            LOGGER.error("Failed to read configuration from URL " + url, ex);
            return false;
        }
    }

    /**
     * Returns a {@link File} reference to the default configuration file. The
     * default configuration is shipped with the application and should always
     * be loaded first to provide a base configuration, so user only has to
     * configure overrides.
     *
     * @return reference to the default configuration file
     */
    private URL getDefaultConfigurationUrl() {
        return getClass().getClassLoader().getResource(DEFAULT_PROPERTIES_RESOURCE);
    }

    /**
     * Returns a {@link File} reference to the local configuration file. The
     * local configuration is by default sought at a well-known location. If
     * set, default location can be overridden.
     *
     * @param filePath optional override for a specific file; default location
     * if null
     * @return reference to the local configuration file
     */
    private File getLocalConfigurationFile(String filePath) {
        if (filePath == null) {
            filePath = System.getProperty("user.home") + File.separator + LOCAL_PROPERTIES_DIRECTORY_NAME + File.separator + LOCAL_PROPERTIES_FILE_NAME;
        }

        return new File(filePath);
    }

    private StorageConfiguration parseStorageConfiguration(Properties properties) {
        StorageConfiguration config = new StorageConfiguration();

        setInteger(properties, "storage.maximumDataFilesPerRequest", config::setMaximumDataFilesPerRequest);
        setString(properties, "storage.transitionalFilesBasePath", config::setTransitionalFilesBasePath);
        setString(properties, "storage.transitionedArchivesBasePath", config::setTransitionedArchivesBasePath);
        setLocalTime(properties, "storage.transitionDailyLocalTime", config::setTransitionDailyLocalTime);
        setZoneId(properties, "storage.transitionTimeZone", config::setTransitionTimeZone);
        setDuration(properties, "storage.transitionPrelude", config::setTransitionPrelude);
        setDuration(properties, "storage.transitionCooldown", config::setTransitionCooldown);

        return config;
    }

    private PackerConfiguration parsePackerConfiguration(Properties properties) {
        PackerConfiguration config = new PackerConfiguration();

        setBoolean(properties, "packer.autoSelectMultiThreading", config::setAutoSelectMultiThreading);

        return config;
    }

    private CamelConfiguration parseCamelConfiguration(Properties properties) {
        CamelConfiguration config = new CamelConfiguration();

        setString(properties, "camel.amqp.host", config::setAmqpHost);
        setInteger(properties, "camel.amqp.port", config::setAmqpPort);
        setString(properties, "camel.amqp.username", config::setAmqpUsername);
        setString(properties, "camel.amqp.password", config::setAmqpPassword);
        setString(properties, "camel.amqp.virtualHost", config::setAmqpVirtualHost);

        setString(properties, "camel.requests.exchange", config::setRequestsExchange);
        setString(properties, "camel.requests.queue", config::setRequestsQueue);
        setDuration(properties, "camel.requests.queueTTL", config::setRequestsQueueTTL);
        setInteger(properties, "camel.requests.consumers", config::setRequestsConsumers);

        return config;
    }

    public CamelConfiguration getCamelConfig() {
        return camelConfig;
    }

    public PackerConfiguration getPackerConfig() {
        return packerConfig;
    }

    public StorageConfiguration getStorageConfig() {
        return storageConfig;
    }

    private void setBoolean(Properties properties, String propertiesKey, Consumer<Boolean> consumer) {
        consumer.accept(Boolean.parseBoolean(properties.getProperty(propertiesKey)));
    }

    private void setString(Properties properties, String propertiesKey, Consumer<String> consumer) {
        consumer.accept(properties.getProperty(propertiesKey));
    }

    private void setInteger(Properties properties, String propertiesKey, Consumer<Integer> consumer) {
        consumer.accept(Integer.parseInt(properties.getProperty(propertiesKey)));
    }

    private void setDuration(Properties properties, String propertiesKey, Consumer<Duration> consumer) {
        consumer.accept(Duration.parse(properties.getProperty(propertiesKey)));
    }

    private void setZoneId(Properties properties, String propertiesKey, Consumer<ZoneId> consumer) {
        consumer.accept(ZoneId.of(properties.getProperty(propertiesKey)));
    }

    private void setLocalTime(Properties properties, String propertiesKey, Consumer<LocalTime> consumer) {
        consumer.accept(LocalTime.parse(properties.getProperty(propertiesKey)));
    }
}
