package org.vatplanner.archiver.local;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * In current implementation, archival/compression of single files to a new
 * .tar.xz per day is performed scripted by a Cron job at specified times. Files
 * before compression are called "transitional" in context of this application
 * as they will transition from single files to archives at a specified point in
 * time.
 *
 * <p>
 * {@link #isTransitional(LocalDate)} can be used to calculate if files for the
 * specified date are still in transitional single-file state or should already
 * have been archived.
 * </p>
 *
 * <p>
 * Affected data must not be accessed during transition to protect the process,
 * use {@link #shouldBackOff(LocalDate)} to check if transition is currently
 * active.
 * </p>
 */
public class TransitionChecker {

    private final LocalTime dailyStartTime;
    private final Duration prelude;
    private final Duration cooldown;
    private final ZoneId timeZone;

    public TransitionChecker(StorageConfiguration configuration) {
        dailyStartTime = configuration.getTransitionDailyLocalTime();
        prelude = configuration.getTransitionPrelude();
        cooldown = configuration.getTransitionCooldown();
        timeZone = configuration.getTransitionTimeZone();
    }

    Instant getNow() {
        return Instant.now();
    }

    /**
     * Calculates if data fetched at given date is still in single-file
     * transitional state or has already been archived.
     *
     * <p>
     * Note that the calculated expectation may not match actual state on
     * storage.
     * </p>
     *
     * @param fetchDate date at which data has been fetched
     * @return true if data for specified date is still in single-file
     * transitional state, false if it has been archived already
     */
    public boolean isTransitional(LocalDate fetchDate) {
        return getNow().isBefore(getTransitionStart(fetchDate));
    }

    /**
     * Calculates if data fetched at given date is currently in transition and
     * must not be accessed to protect the archival process.
     *
     * @param fetchDate date at which data has been fetched
     * @return true if transition is currently being performed and data must not
     * be accessed, false if data is supposed to be accessible
     */
    public boolean shouldBackOff(LocalDate fetchDate) {
        Instant transitionStart = getTransitionStart(fetchDate);
        Instant backOffStart = transitionStart.minus(prelude);
        Instant backOffEnd = transitionStart.plus(cooldown);
        Instant now = getNow();

        return now.isAfter(backOffStart) && now.isBefore(backOffEnd);
    }

    /**
     * Calculates the start of transition process for data fetched at given
     * date.
     *
     * @param fetchDate date at which data has been fetched
     * @return start timestamp of transition process
     */
    private Instant getTransitionStart(LocalDate fetchDate) {
        // TODO: test what happens at impossible times (transition scheduled during DST change day for skipped hour)
        return fetchDate
                .plusDays(1)
                .atTime(dailyStartTime)
                .atZone(timeZone)
                .toInstant();
    }

    // FIXME: unit test
}
