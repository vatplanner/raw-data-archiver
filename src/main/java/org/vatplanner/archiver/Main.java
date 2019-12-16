package org.vatplanner.archiver;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.camel.RawDataArchiverRouteBuilder;
import org.vatplanner.archiver.local.Loader;
import org.vatplanner.archiver.local.TransitionChecker;
import org.vatplanner.archiver.remote.PackerFactory;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        // load config
        String configPath = null;
        if (args.length > 0) {
            configPath = args[0];
        }
        Configuration config = new Configuration(configPath);

        // set up services
        TransitionChecker transitionChecker = new TransitionChecker(config.getStorageConfig());
        Loader loader = new Loader(config.getStorageConfig(), transitionChecker);
        PackerFactory packerFactory = new PackerFactory(config.getPackerConfig());

        // start Camel
        CamelContext camelContext = null;
        try {
            LOGGER.info("Configuring Camel...");
            camelContext = new DefaultCamelContext();
            camelContext.addRoutes(new RawDataArchiverRouteBuilder(camelContext, config.getCamelConfig(), loader, packerFactory));

            LOGGER.info("Starting Camel...");
            camelContext.start();
            while (camelContext.getStatus().isStarting()) {
                Thread.yield();
            }

            LOGGER.info("Camel started...");
        } catch (Exception ex) {
            LOGGER.error("Error while running Camel", ex);
            System.exit(1);
        }

        Thread.sleep(10000);
    }
}
