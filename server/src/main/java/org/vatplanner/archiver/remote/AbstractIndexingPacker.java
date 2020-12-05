package org.vatplanner.archiver.remote;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.vatplanner.archiver.common.RawDataFile;
import org.vatplanner.archiver.common.RemoteMetaDataContainerJsonKey;
import org.vatplanner.archiver.common.RemoteMetaDataFileJsonKey;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * Base class for {@link Packer}s storing content numerically enumerated with an
 * additional meta-data file. Generation of meta data file is provided by this
 * class. Files are indexed as file names are requested or when meta data is
 * queried for. No additional data must be added after meta data has been
 * requested as otherwise meta data would not know all files.
 * <p>
 * Methods provided by this base class are thread-safe.
 * </p>
 */
public abstract class AbstractIndexingPacker implements Packer {

    private byte[] metaData;
    private final Map<RawDataFile, Integer> indexesByOriginal = new HashMap<>();

    private static final int FORMAT_VERSION = 1;
    private static final String CONTENT = "StatusDataFile";

    public static final String META_DATA_FILE_NAME = "meta.json";

    /**
     * Returns the index addressing the given file in meta data. Unknown files are
     * automatically assigned a new index number. Subsequent calls for the same file
     * will yield the same index.
     *
     * @param original file to look up from index
     * @return index number assigned to the file
     */
    private int getFileIndex(RawDataFile original) {
        synchronized (indexesByOriginal) {
            return indexesByOriginal.computeIfAbsent(original, o -> indexesByOriginal.size() + 1);
        }
    }

    /**
     * Returns a numeric file name for given file. Unknown files are automatically
     * assigned a new index number/file name. Subsequent calls for the same file
     * will yield the same index.
     *
     * @param original file to look up from index and generate a name for
     * @return internal numeric file name
     */
    protected String getFileName(RawDataFile original) {
        return String.format("%08d.dat", getFileIndex(original));
    }

    /**
     * Returns the JSON meta data as a byte array encoded in UTF8. No new files must
     * be added to the index after calling this method. Encoded result will be
     * cached and reused on consecutive calls. Yet unknown files will be indexed
     * during creation of meta data.
     *
     * @param originals original files to encode meta data for
     * @return UTF8-encoded JSON meta data for given files; cached on consecutive
     *         calls
     */
    protected byte[] getMetaData(Collection<RawDataFile> originals) {
        synchronized (this) {
            if (metaData != null) {
                return metaData;
            }

            metaData = encodeMetaData(originals);
        }

        return metaData;
    }

    /**
     * Generates and encodes JSON meta data for the full container in UTF8. Unknown
     * files will be indexed during creation of meta data.
     *
     * @param originals original files to encode meta data for
     * @return UTF8-encoded JSON meta data for given files
     */
    private byte[] encodeMetaData(Collection<RawDataFile> originals) {
        JsonObject files = new JsonObject();
        for (RawDataFile original : originals) {
            files.put(getFileName(original), buildFileMetaData(original));
        }

        JsonObject container = new JsonObject();
        container.put(RemoteMetaDataContainerJsonKey.FORMAT_VERSION.getKey(), FORMAT_VERSION);
        container.put(RemoteMetaDataContainerJsonKey.CONTENT.getKey(), CONTENT);
        container.put(RemoteMetaDataContainerJsonKey.FILES.getKey(), files);

        return serializeJson(container);
    }

    /**
     * Generates JSON meta data for a single file.
     *
     * @param original original files to generate meta data for
     * @return JSON meta data for given file
     */
    private JsonObject buildFileMetaData(RawDataFile original) {
        JsonObject file = new JsonObject();
        file.put(RemoteMetaDataFileJsonKey.FETCH_TIME.getKey(), original.getFetchTime().toString());
        putFileMetaDataIfNotNull(file, RemoteMetaDataFileJsonKey.FETCH_NODE, original.getFetchNode());
        putFileMetaDataIfNotNull(file, RemoteMetaDataFileJsonKey.FETCH_URL_REQUESTED, original.getFetchUrlRequested());
        putFileMetaDataIfNotNull(file, RemoteMetaDataFileJsonKey.FETCH_URL_RETRIEVED, original.getFetchUrlRetrieved());
        return file;
    }

    /**
     * Stores the specified value to the given {@link JsonObject} if the value is
     * not null. Nothing will be stored if the value is null.
     *
     * @param json JSON object to add value to
     * @param key key under which value should be stored
     * @param value value to be added unless null
     */
    private void putFileMetaDataIfNotNull(JsonObject json, RemoteMetaDataFileJsonKey key, Object value) {
        if (value == null) {
            return;
        }

        json.put(key.getKey(), value);
    }

    /**
     * Serializes the given {@link JsonObject} to a UTF8-encoded byte array.
     *
     * @param json object to serialize
     * @return object as UTF8-encoded byte array
     */
    private byte[] serializeJson(JsonObject json) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            json.toJson(writer);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize JSON meta data", ex);
        }

        return baos.toByteArray();
    }

}
