package org.vatplanner.archiver.local;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.RawDataFile;

/**
 * Loads previously fetched data from storage back into memory.
 *
 * <p>
 * Data is split into different files:
 * </p>
 *
 * <ol>
 * <li>a raw file of retrieved remote information</li>
 * <li>a meta data file to hold additional local information</li>
 * </ol>
 *
 * <p>
 * Data is stored in two different ways:
 * </p>
 *
 * <ol>
 * <li><i>transitional:</i> initially, data is stored in single files unpacked
 * for up to one day</li>
 * <li><i>transitioned:</i> once per day all transitional files for the previous
 * day are compressed to a .tar.xz file stored in a different location</li>
 * </ol>
 *
 * <p>
 * Loading data from storage requires both locations/formats to be taken into
 * account and meta data to be reintegrated.
 * </p>
 *
 * <p>
 * While archiving of data is currently not performed by this application but
 * external processes, {@link TransitionChecker} is queried to check state and
 * access to files. Access to previous day data will be denied around the time
 * transition process is scheduled; see {@link TransitionChecker} for details.
 * </p>
 */
public class Loader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class);

    private final int maximumDataFilesPerRequest;
    private final TransitionChecker transitionChecker;
    private final File transitionedBasePath;
    private final File transitionalBasePath;

    private final CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
    private final ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();

    private static final Pattern PATTERN_FETCHED_FILENAME = Pattern.compile("^(\\d{4})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])T([01][0-9]|2[0-3])([0-5][0-9])([0-5][0-9])Z_.*");
    private static final int PATTERN_FETCHED_FILENAME_YEAR = 1;
    private static final int PATTERN_FETCHED_FILENAME_MONTH = 2;
    private static final int PATTERN_FETCHED_FILENAME_DAY = 3;
    private static final int PATTERN_FETCHED_FILENAME_HOUR = 4;
    private static final int PATTERN_FETCHED_FILENAME_MINUTE = 5;
    private static final int PATTERN_FETCHED_FILENAME_SECOND = 6;

    private static final Pattern PATTERN_DIRECTORY_YEAR = Pattern.compile("^\\d{4}$");

    private static final Pattern PATTERN_DIRECTORY_MONTH = Pattern.compile("^(0[1-9]|1[0-2])$");

    private static final Pattern PATTERN_ARCHIVE = Pattern.compile("^(\\d{4})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])\\.tar\\.xz$");
    private static final int PATTERN_ARCHIVE_DAY = 3;

    private static final DateTimeFormatter FORMATTER_ARCHIVE_NAME = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ARCHIVE_EXTENSION = ".tar.xz";

    private static final int MAXIMUM_LOCAL_DATE_YEAR = 9999; // live long and prosper...
    private static final Instant MINIMUM_LOCAL_DATE_INSTANT = Instant.EPOCH;
    private static final Instant MAXIMUM_LOCAL_DATE_INSTANT = ZonedDateTime.of(MAXIMUM_LOCAL_DATE_YEAR, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")).toInstant();

    private static final Charset CHARACTER_SET_META_DATA = StandardCharsets.UTF_8;

    public Loader(StorageConfiguration config, TransitionChecker transitionChecker) {
        maximumDataFilesPerRequest = config.getMaximumDataFilesPerRequest();
        this.transitionChecker = transitionChecker;

        transitionalBasePath = config.getTransitionalFilesBasePath();
        transitionedBasePath = config.getTransitionedArchivesBasePath();
    }

    /**
     * Loads and returns all data fetched between given timestamps. The maximum
     * number of files is limited by the hard maximum specified in application
     * configuration. The result is ordered by oldest fetched entry first.
     *
     * @param earliestFetchTime earliest fetch time to include in result
     * @param latestFetchTime latest fetch time to include in result
     * @return at most the configured number of files fetched between given
     * timestamps (ascending order); null on error
     */
    public List<RawDataFile> load(Instant earliestFetchTime, Instant latestFetchTime) {
        return load(earliestFetchTime, latestFetchTime, maximumDataFilesPerRequest);
    }

    /**
     * Loads and returns all data fetched between given timestamps. The
     * specified maximum number of files is only effective if it is less than
     * the hard maximum limit specified in application configuration. The result
     * is ordered by oldest fetched entry first.
     *
     * @param earliestFetchTime earliest fetch time to include in result
     * @param latestFetchTime latest fetch time to include in result
     * @param fileLimit maximum number of files to be returned; may be
     * restricted further by configuration
     * @return at most the requested number of files fetched between given
     * timestamps (ascending order); null on error
     */
    public List<RawDataFile> load(Instant earliestFetchTime, Instant latestFetchTime, int fileLimit) {
        // TODO: catch IOException, log and return null instead

        int remainingFileLimit = Integer.min(fileLimit, maximumDataFilesPerRequest);

        LOGGER.debug("Loading at most {} files (requested {}) fetched between {} and {}", remainingFileLimit, fileLimit, earliestFetchTime, latestFetchTime);

        List<RawDataFile> loaded = new ArrayList<>();

        try {
            LocalDate transitionedFetchDate = findEffectiveEarliestTransitionedFetchDate(earliestFetchTime);
            while ((remainingFileLimit > 0) && shouldLoadFromTransitionedFile(transitionedFetchDate, latestFetchTime)) {
                Collection<RawDataFile> dataFiles = loadFromTransitionedFile(transitionedFetchDate, earliestFetchTime, latestFetchTime);
                loaded.addAll(dataFiles);

                remainingFileLimit -= dataFiles.size();
                transitionedFetchDate = transitionedFetchDate.plusDays(1);
            }

            loaded.addAll(loadFromTransitionalFiles(earliestFetchTime, latestFetchTime, remainingFileLimit));
        } catch (IOException ex) {
            LOGGER.warn(
                    "Loading data failed; requested at most " + fileLimit + " files from "
                    + (earliestFetchTime != null ? earliestFetchTime.toString() : "null") + " to "
                    + (latestFetchTime != null ? latestFetchTime.toString() : "null"),
                    ex
            );
            return null;
        }

        loaded.sort(Comparator.comparing(RawDataFile::getFetchTime));

        LOGGER.debug("Loaded total of {} files", loaded.size());

        // FIXME: does not seem to be effective file limit (limit by min maximumDataFilesPerRequest?)
        if (loaded.size() > fileLimit) {
            loaded = loaded.subList(0, fileLimit);
        }

        LOGGER.debug("Returning {} files", loaded.size());

        return loaded;
    }

    /**
     * Limits the given timestamp for the earliest fetch time to be retrieved by
     * the oldest available transitioned (packed) archived file, returning only
     * the date.
     *
     * @param earliestFetchTime earliest fetch time to retrieve
     * @return fetch date limited by available transitioned data
     */
    private LocalDate findEffectiveEarliestTransitionedFetchDate(Instant earliestFetchTime) {
        LocalDate earliestArchivedFetchDate;
        try {
            earliestArchivedFetchDate = findEarliestTransitionedFetchDate();
        } catch (IOException ex) {
            LOGGER.warn("Unable to determine earliest transitioned fetch date, assuming no data.", ex);
            return null;
        }

        return max(earliestFetchTime, earliestArchivedFetchDate);
    }

    /**
     * Creates a new {@link Predicate} to check if a String matches the given
     * Pattern.
     *
     * @param pattern Pattern expected to be matched
     * @return Predicate checking if a String matches the given Pattern
     */
    private Predicate<String> patternMatches(Pattern pattern) {
        // TODO: move to utils
        return s -> pattern.matcher(s).matches();
    }

    /**
     * Searches the earliest available date available from transitioned (packed)
     * data. Later data may be available as transitional files.
     *
     * @return date of earliest available transitioned data; null if no
     * transitioned data available
     * @throws IOException
     */
    private LocalDate findEarliestTransitionedFetchDate() throws IOException {
        int year = findNumericDirectoryNameMinimum(transitionedBasePath, PATTERN_DIRECTORY_YEAR)
                .orElse(-1);
        if (year < 0) {
            LOGGER.warn("Transitioned data appears to be missing year folders; this indicates there is no transitioned data at all! Check if that is correct.");
            return null;
        }

        int month = findNumericDirectoryNameMinimum(getTransitionedDirectory(year), PATTERN_DIRECTORY_MONTH)
                .orElse(-1);
        if (month < 0) {
            LOGGER.error("Transitioned data appears to be missing month folders for year {}; this indicates corrupted folder structure! Data will be inaccessible.", year);
            return null;
        }

        int day = findNumericFileNameMinimum(getTransitionedDirectory(year, month), PATTERN_ARCHIVE, PATTERN_ARCHIVE_DAY)
                .orElse(-1);
        if (day < 0) {
            LOGGER.error("Transitioned data appears to be missing day files for month {}, year {}; this indicates corrupted folder structure! Data will be inaccessible.", month, year);
            return null;
        }

        return LocalDate.of(year, month, day);
    }

    /**
     * Returns a reference to the expected directory holding transitioned data
     * for given year.
     *
     * @param year year to reference directory for
     * @return reference to the expected directory
     * @throws IOException
     */
    private File getTransitionedDirectory(int year) throws IOException {
        return new File(String.format("%s%s%04d", transitionedBasePath.getCanonicalPath(), File.separator, year));
    }

    /**
     * Returns a reference to the expected directory holding transitioned data
     * for given month of year.
     *
     * @param year year to reference directory for
     * @param month month to reference directory for
     * @return reference to the expected directory
     * @throws IOException
     */
    private File getTransitionedDirectory(int year, int month) throws IOException {
        return new File(String.format("%s%s%04d%s%02d", transitionedBasePath.getCanonicalPath(), File.separator, year, File.separator, month));
    }

    /**
     * Returns a reference to the expected archive file holding transitioned
     * data for given date.
     *
     * @param fetchDate date to reference file for
     * @return reference to the expected archive file
     * @throws IOException
     */
    private File getTransitionedArchiveFile(LocalDate fetchDate) throws IOException {
        return new File(String.format("%s%s%04d%s%02d%s%s", transitionedBasePath.getCanonicalPath(), File.separator, fetchDate.getYear(), File.separator, fetchDate.getMonthValue(), File.separator, getTransitionedArchiveFileName(fetchDate)));
    }

    /**
     * Returns the expected archive file name to be used for data of given fetch
     * date.
     *
     * @param fetchDate date to construct file name for
     * @return expected archive file name for transitioned data of given date
     */
    private String getTransitionedArchiveFileName(LocalDate fetchDate) {
        return FORMATTER_ARCHIVE_NAME.format(fetchDate) + ARCHIVE_EXTENSION;
    }

    /**
     * Searches the given directory for the child directory with smallest
     * numeric name.
     *
     * @param directory parent directory to search in
     * @param pattern Pattern of accepted directory names (not used for
     * extraction)
     * @return number of smallest directory name; empty if not found, never null
     */
    private OptionalInt findNumericDirectoryNameMinimum(File directory, Pattern pattern) {
        if (!directory.exists() || !directory.isDirectory()) {
            LOGGER.warn("Directory for transitioned data does not exist: {}", directory);
            return OptionalInt.empty();
        }

        return Arrays.stream(directory.listFiles())
                .filter(File::isDirectory)
                .map(File::getName)
                .filter(patternMatches(pattern))
                .mapToInt(Integer::parseInt)
                .min();
    }

    /**
     * Searches the given directory for the file with smallest numeric name.
     *
     * @param directory directory to search in
     * @param pattern Pattern to apply to file names
     * @param matcherGroupIndex Pattern Matcher group index to extract numeric
     * value from
     * @return number of smallest extraction from file name; empty if not found,
     * never null
     */
    private OptionalInt findNumericFileNameMinimum(File directory, Pattern pattern, int matcherGroupIndex) {
        if (!directory.exists() || !directory.isDirectory()) {
            LOGGER.warn("Directory for transitioned data does not exist: {}", directory);
            return OptionalInt.empty();
        }

        return Arrays.stream(directory.listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(matcherGroupIndex))
                .mapToInt(Integer::parseInt)
                .min();
    }

    /**
     * Returns the maximum (later point in time) of both inputs.
     *
     * @param a first input
     * @param b second input
     * @return date of later input of both
     */
    private LocalDate max(Instant a, LocalDate b) {
        // TODO: move to utils

        if ((a == null) || (b == null)) {
            return null;
        }

        LocalDate aDate = toLocalDateUTC(a);

        return max(aDate, b);
    }

    /**
     * Returns the maximum (later point in time) of both inputs.
     *
     * @param a first input
     * @param b second input
     * @return date of later input of both
     */
    private LocalDate max(LocalDate a, LocalDate b) {
        // TODO: move to utils

        if ((a == null) || (b == null)) {
            return null;
        }

        if (a.isAfter(b)) {
            return a;
        }

        return b;
    }

    /**
     * Returns the UTC date of given timestamp. An interesting quirk of the Java
     * date/time API is that Instants can outgrow LocalDate so only a limited
     * value range is supported on conversion to dates. The result of this
     * method is therefore limited to a hard-coded range of dates.
     *
     * @param timestamp timestamp to convert
     * @return UTC date of timestamp, limited to a reasonable value range
     */
    private LocalDate toLocalDateUTC(Instant timestamp) {
        // TODO: move to utils

        // clamp to defined range because LocalDate is unable to represent full
        // value range of Instant
        if (MINIMUM_LOCAL_DATE_INSTANT.isAfter(timestamp)) {
            timestamp = MINIMUM_LOCAL_DATE_INSTANT;
        } else if (MAXIMUM_LOCAL_DATE_INSTANT.isBefore(timestamp)) {
            timestamp = MAXIMUM_LOCAL_DATE_INSTANT;
        }

        return timestamp.atOffset(ZoneOffset.UTC).toLocalDate();
    }

    private Collection<RawDataFile> loadFromTransitionalFiles(Instant earliestFetchTime, Instant latestFetchTime, int fileLimit) throws IOException {
        // TODO: documentation, random order

        Map<Instant, RawDataFile> loaded = new HashMap<>();

        List<File> files = Arrays.asList(transitionalBasePath.listFiles());

        // sort in order to be able to check file limit
        files.sort(Comparator.comparing(File::getName));

        for (File file : files) {
            // TODO: check if there is some readable way to de-duplicate this section

            String fileName = file.getName();
            Instant fetchTime = extractFileFetchTimestamp(fileName);

            // skip if out of requested range
            if (!inRange(fetchTime, earliestFetchTime, latestFetchTime)) {
                continue;
            }

            // skip unsupported files
            FetchedFileType fileType = FetchedFileType.byFileName(fileName);
            if (fileType == null) {
                LOGGER.debug("Skipping unsupported file: {}", file);
                continue;
            }

            // stop if new timestamp is encountered but limit is reached
            // (files are processed ordered, so we can reliably quit early when
            // a new timestamp is encountered)
            if (!loaded.containsKey(fetchTime) && (loaded.size() >= fileLimit)) {
                break;
            }

            RawDataFile rawDataFile = loaded.computeIfAbsent(fetchTime, RawDataFile::new);

            try {
                byte[] bytes = readFile(file);

                switch (fileType) {
                    case META_DATA:
                        loadMetaData(rawDataFile, bytes);
                        break;

                    case RAW_VATSIM_DATA_FILE:
                        rawDataFile.setData(bytes);
                        break;

                    default:
                        LOGGER.warn("File type {} read from {} is not taken into account!", fileType, file);
                        break;
                }
            } catch (IOException ex) {
                throw new IOException("failed to read data from " + file.getCanonicalPath(), ex);
            }
        }

        return loaded.values();
    }

    /**
     * Extracts the fetch timestamp from given file name of an archived file.
     * Actual file-system timestamps are not taken into account as they have
     * nothing to do with the timestamp of fetching data.
     *
     * @param filename file name to extract time from
     * @return timestamp extracted from filename; null if unavailable
     */
    private Instant extractFileFetchTimestamp(String filename) {
        Matcher matcher = PATTERN_FETCHED_FILENAME.matcher(filename);
        if (!matcher.matches()) {
            return null;
        }

        int year = Integer.parseInt(matcher.group(PATTERN_FETCHED_FILENAME_YEAR));
        int month = Integer.parseInt(matcher.group(PATTERN_FETCHED_FILENAME_MONTH));
        int day = Integer.parseInt(matcher.group(PATTERN_FETCHED_FILENAME_DAY));
        int hour = Integer.parseInt(matcher.group(PATTERN_FETCHED_FILENAME_HOUR));
        int minute = Integer.parseInt(matcher.group(PATTERN_FETCHED_FILENAME_MINUTE));
        int second = Integer.parseInt(matcher.group(PATTERN_FETCHED_FILENAME_SECOND));

        return LocalDateTime.of(year, month, day, hour, minute, second).atOffset(ZoneOffset.UTC).toInstant();
    }

    /**
     * Checks if the given actual timestamp is in range between specified
     * earliest and latest timestamps.
     *
     * @param actual actual timestamp to be checked
     * @param earliest earliest valid timestamp, lower end of value range
     * (inclusive)
     * @param latest latest valid timestamp, upper end of value range
     * (inclusive)
     * @return true if actual value is in range, false if out of range
     */
    private boolean inRange(Instant actual, Instant earliest, Instant latest) {
        return earliest.compareTo(actual) <= 0 && actual.compareTo(latest) <= 0;
    }

    /**
     * Determines if data for the given fetch date should be attempted to be
     * loaded from a transitioned file. The decision is based on two factors:
     * <ul>
     * <li>Is the date in range of requested data to be loaded?</li>
     * <li>Is data for given date expected to have been transitioned?</li>
     * </ul>
     *
     * @param fetchDate date to check access for
     * @param latestFetchTime fetch timestamp of latest file requested to be
     * loaded
     * @return true if loading should be attempted from transitioned file, false
     * if not
     */
    private boolean shouldLoadFromTransitionedFile(LocalDate fetchDate, Instant latestFetchTime) {
        // check if out of requested range
        LocalDate latestFetchDate = toLocalDateUTC(latestFetchTime);
        if (fetchDate.isAfter(latestFetchDate)) {
            return false;
        }

        // check if data cannot have been transitioned yet
        return !transitionChecker.isTransitional(fetchDate);
    }

    /**
     * Loads data from transitioned files. Loading transitioned files means
     * decompressing and reading an archive. No specific order of data should be
     * assumed from reading an archive, so it needs to be sorted and any
     * limitation of maximum number of files needs to be taken care of outside
     * of this method.
     *
     * @param fetchDate fetch date of transitioned data (determines archive to
     * be opened)
     * @param earliestFetchTime earliest fetch time to include in result
     * @param latestFetchTime latest fetch time to include in result
     * @return all files matching given time range in random order
     * @throws IOException
     */
    private Collection<RawDataFile> loadFromTransitionedFile(LocalDate fetchDate, Instant earliestFetchTime, Instant latestFetchTime) throws IOException {
        Map<Instant, RawDataFile> loaded = new HashMap<>();

        File archiveFile = getTransitionedArchiveFile(fetchDate);

        LOGGER.debug("opening transitional file {}", archiveFile);

        if (!archiveFile.exists() || !archiveFile.canRead()) {
            throw new RuntimeException("expected archive file " + archiveFile.getCanonicalPath() + " does not exist or is inaccessible, unable to load data");
        }

        try (
                FileInputStream fis = new FileInputStream(archiveFile);
                CompressorInputStream cis = compressorStreamFactory.createCompressorInputStream(CompressorStreamFactory.XZ, fis);
                ArchiveInputStream ais = archiveStreamFactory.createArchiveInputStream(ArchiveStreamFactory.TAR, cis); //
                ) {
            ArchiveEntry entry;
            while ((entry = ais.getNextEntry()) != null) {
                String fileName = entry.getName();
                Instant fetchTime = extractFileFetchTimestamp(fileName);

                // skip if out of requested range
                if (!inRange(fetchTime, earliestFetchTime, latestFetchTime)) {
                    continue;
                }

                // skip unsupported files
                FetchedFileType fileType = FetchedFileType.byFileName(fileName);
                if (fileType == null) {
                    LOGGER.debug("Skipping unsupported file {} read from {}", fileName, archiveFile);
                    continue;
                }

                RawDataFile rawDataFile = loaded.computeIfAbsent(fetchTime, RawDataFile::new);
                byte[] bytes = readArchiveEntry(ais, entry);

                switch (fileType) {
                    case META_DATA:
                        loadMetaData(rawDataFile, bytes);
                        break;

                    case RAW_VATSIM_DATA_FILE:
                        rawDataFile.setData(bytes);
                        break;

                    default:
                        LOGGER.warn("File type {} read from {} of {} is not taken into account!", fileType, fileName, archiveFile);
                        break;
                }
            }
        } catch (Exception ex) {
            throw new IOException("failed to extract data from archive " + archiveFile.getCanonicalPath(), ex);
        }

        return loaded.values();
    }

    /**
     * Reads all data of a single {@link ArchiveEntry}.
     *
     * @param ais {@link ArchiveInputStream} to read from
     * @param entry {@link ArchiveEntry} to read data for
     * @return data of given {@link ArchiveEntry}
     * @throws IOException
     */
    private byte[] readArchiveEntry(ArchiveInputStream ais, ArchiveEntry entry) throws IOException {
        byte[] bytes = new byte[(int) entry.getSize()];
        int offset = 0;
        while (offset < bytes.length) {
            offset += ais.read(bytes, offset, bytes.length - offset);
        }

        return bytes;
    }

    /**
     * Reads all data of given {@link File}.
     *
     * @param file file to read
     * @return data of given file
     * @throws IOException
     */
    private byte[] readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int offset = 0;
            while (offset < bytes.length) {
                offset += fis.read(bytes, offset, bytes.length - offset);
            }

            return bytes;
        }
    }

    /**
     * Loads meta data from the file whose contents are given as a byte array
     * and copies available information to the {@link RawDataFile}. Since meta
     * data contains the fetch time again, it will be used to verify the
     * previously initialized value of {@link RawDataFile#fetchTime} for
     * consistency. An exception will be raised in case of an inconsistency.
     *
     * @param rawDataFile file linked to meta data
     * @param bytes content of meta data file
     * @throws IOException
     */
    private void loadMetaData(RawDataFile rawDataFile, byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                InputStreamReader isr = new InputStreamReader(bais, CHARACTER_SET_META_DATA); //
                ) {
            JsonObject json = (JsonObject) Jsoner.deserialize(isr);

            rawDataFile.setFetchUrlRequested(json.getString(LocalMetaDataJsonKeys.FETCH_URL_REQUESTED));

            String actualFetchTimestampString = json.getString(LocalMetaDataJsonKeys.FETCH_TIME);
            Instant actualFetchTime = Instant.parse(actualFetchTimestampString);
            if (!rawDataFile.getFetchTime().equals(actualFetchTime)) {
                throw new IOException("inconsistent data; fetch time was " + actualFetchTime + " according to meta data but has been indexed as " + rawDataFile.getFetchTime());
            }
        } catch (JsonException ex) {
            throw new IOException("failed to deserialize meta data", ex);
        }
    }
}
