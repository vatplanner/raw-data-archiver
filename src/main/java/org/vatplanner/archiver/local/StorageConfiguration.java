package org.vatplanner.archiver.local;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Holds configuration values for this application.
 */
public class StorageConfiguration {

    /**
     * Returns the maximum number of data files allowed to be loaded into memory
     * per request.
     *
     * @return maximum number of data files allowed to load per request
     */
    public int getMaximumDataFilesPerRequest() {
        // FIXME: read from config file; limited by maximum number of files in ZIP as well as reasonable resource limit to avoid OOM and deliver answer before timeout
        return 10 * 24 * 60 / 2; // 10 days of data at 2 minute interval
    }

    /**
     * Returns the root directory holding all transitional (not transitioned)
     * single files. Expected storage structure beneath this directory are only
     * files named by UTC timestamp and _meta.json or _vatsim-data.txt suffix
     * with according content.
     *
     * @return directory holding all transitional single files
     */
    public File getTransitionalFilesBasePath() {
        // FIXME: read from config file
        return new File(System.getenv("USERPROFILE") + File.separator + "Desktop" + File.separator + "vatsim" + File.separator + "data");
    }

    /**
     * Returns the root directory holding all archived (transitioned) files.
     * Expected storage structure beneath this directory is:
     * YYYY/MM/YYYYMMDD.tar.xz
     *
     * @return directory holding all archives
     */
    public File getTransitionedArchivesBasePath() {
        // FIXME: read from config file
        return new File(System.getenv("USERPROFILE") + File.separator + "Desktop" + File.separator + "vatsim" + File.separator + "archive");
    }

    /**
     * Returns the daily local time at which transition from single files to
     * archives is expected to be performed. Transition is triggered by a Cron
     * job, so the returned time is local to the system performing the
     * compression.
     *
     * @return daily local time at which transition is supposed to happen
     * @see TransitionChecker
     */
    public LocalTime getTransitionDailyLocalTime() {
        // FIXME: read from config file
        return LocalTime.of(3, 41);
    }

    /**
     * Returns the time before scheduled start of transition from single files
     * to archives at which processes should start to back off from
     * to-be-compressed files.
     *
     * @return time to start backing off from to-be-compressed files
     * @see TransitionChecker
     */
    public Duration getTransitionPrelude() {
        // FIXME: read from config file
        return Duration.ofSeconds(30);
    }

    /**
     * Returns the time after scheduled start of transition from single files to
     * archives at which processes should still back off from to-be-compressed
     * files. Since previous-day data was to be compressed, that archive is also
     * not to be accessed during this time period.
     *
     * @return time after start of transition to keep off to-be-compressed files
     * and previous-day archive
     * @see TransitionChecker
     */
    public Duration getTransitionCooldown() {
        // FIXME: read from config file
        return Duration.ofMinutes(3);
    }

    /**
     * Returns the time zone applicable to the system in charge of performing
     * the transition from single files to archives.
     *
     * @return time zone of system performing transition
     * @see TransitionChecker
     */
    public ZoneId getTransitionTimeZone() {
        // FIXME: read from config file
        return ZoneId.systemDefault();
    }
}
