package org.vatplanner.archiver.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configuration options relevant to packers.
 */
public class PackerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackerConfiguration.class);

    private boolean autoSelectMultiThreading = false;

    /**
     * Determines if multi-threaded methods should be auto-selected in case no
     * threading has been specified explicitely.
     *
     * @return true if multi-threading should be selected, false if
     * single-threaded implementations should be used
     */
    public boolean shouldAutoSelectMultiThreading() {
        return autoSelectMultiThreading;
    }

    /**
     * Sets whether multi-threaded implementations should be used in case not
     * threading has been specified explicitely.
     *
     * @param autoSelectMultiThreading true if multi-threading should be
     * selected, false if single-threaded implementations should be used
     * @return this instance for method-chaining
     */
    public PackerConfiguration setAutoSelectMultiThreading(boolean autoSelectMultiThreading) {
        LOGGER.debug("setting autoSelectMultiThreading to {}", autoSelectMultiThreading);
        this.autoSelectMultiThreading = autoSelectMultiThreading;
        return this;
    }

}
