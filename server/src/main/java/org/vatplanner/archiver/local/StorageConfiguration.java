package org.vatplanner.archiver.local;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configuration values for this application.
 */
public class StorageConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConfiguration.class);

    private int maximumDataFilesPerRequest;
    private String transitionalFilesBasePath;
    private String transitionedArchivesBasePath;
    private LocalTime transitionDailyLocalTime;
    private Duration transitionPrelude;
    private Duration transitionCooldown;
    private ZoneId transitionTimeZone;

    /**
     * Returns the maximum number of data files allowed to be loaded into memory per
     * request.
     *
     * @return maximum number of data files allowed to load per request
     */
    public int getMaximumDataFilesPerRequest() {
        return maximumDataFilesPerRequest;
    }

    public StorageConfiguration setMaximumDataFilesPerRequest(int maximumDataFilesPerRequest) {
        LOGGER.debug("setting maximumDataFilesPerRequest to {}", maximumDataFilesPerRequest);
        this.maximumDataFilesPerRequest = maximumDataFilesPerRequest;
        return this;
    }

    /**
     * Returns the root directory holding all transitional (not transitioned) single
     * files. Expected storage structure beneath this directory are only files named
     * by UTC timestamp and _meta.json or _vatsim-data.txt suffix with according
     * content.
     *
     * @return directory holding all transitional single files
     */
    public File getTransitionalFilesBasePath() {
        return new File(transitionalFilesBasePath);
    }

    public StorageConfiguration setTransitionalFilesBasePath(String transitionalFilesBasePath) {
        LOGGER.debug("setting transitionalFilesBasePath to {}", transitionalFilesBasePath);
        this.transitionalFilesBasePath = transitionalFilesBasePath;
        return this;
    }

    /**
     * Returns the root directory holding all archived (transitioned) files.
     * Expected storage structure beneath this directory is: YYYY/MM/YYYYMMDD.tar.xz
     *
     * @return directory holding all archives
     */
    public File getTransitionedArchivesBasePath() {
        return new File(transitionedArchivesBasePath);
    }

    public StorageConfiguration setTransitionedArchivesBasePath(String transitionedArchivesBasePath) {
        LOGGER.debug("setting transitionedArchivesBasePath to {}", transitionedArchivesBasePath);
        this.transitionedArchivesBasePath = transitionedArchivesBasePath;
        return this;
    }

    /**
     * Returns the daily local time at which transition from single files to
     * archives is expected to be performed. Transition is triggered by a Cron job,
     * so the returned time is local to the system performing the compression.
     *
     * @return daily local time at which transition is supposed to happen
     * @see TransitionChecker
     */
    public LocalTime getTransitionDailyLocalTime() {
        return transitionDailyLocalTime;
    }

    public StorageConfiguration setTransitionDailyLocalTime(LocalTime transitionDailyLocalTime) {
        LOGGER.debug("setting transitionDailyLocalTime to {}", transitionDailyLocalTime);
        this.transitionDailyLocalTime = transitionDailyLocalTime;
        return this;
    }

    /**
     * Returns the time before scheduled start of transition from single files to
     * archives at which processes should start to back off from to-be-compressed
     * files.
     *
     * @return time to start backing off from to-be-compressed files
     * @see TransitionChecker
     */
    public Duration getTransitionPrelude() {
        return transitionPrelude;
    }

    public StorageConfiguration setTransitionPrelude(Duration transitionPrelude) {
        LOGGER.debug("setting transitionPrelude to {}", transitionPrelude);
        this.transitionPrelude = transitionPrelude;
        return this;
    }

    /**
     * Returns the time after scheduled start of transition from single files to
     * archives at which processes should still back off from to-be-compressed
     * files. Since previous-day data was to be compressed, that archive is also not
     * to be accessed during this time period.
     *
     * @return time after start of transition to keep off to-be-compressed files and
     *         previous-day archive
     * @see TransitionChecker
     */
    public Duration getTransitionCooldown() {
        return transitionCooldown;
    }

    public StorageConfiguration setTransitionCooldown(Duration transitionCooldown) {
        LOGGER.debug("setting transitionCooldown to {}", transitionCooldown);
        this.transitionCooldown = transitionCooldown;
        return this;
    }

    /**
     * Returns the time zone applicable to the system in charge of performing the
     * transition from single files to archives.
     *
     * @return time zone of system performing transition
     * @see TransitionChecker
     */
    public ZoneId getTransitionTimeZone() {
        return transitionTimeZone;
    }

    public StorageConfiguration setTransitionTimeZone(ZoneId transitionTimeZone) {
        LOGGER.debug("setting transitionTimeZone to {}", transitionTimeZone);
        this.transitionTimeZone = transitionTimeZone;
        return this;
    }
}
