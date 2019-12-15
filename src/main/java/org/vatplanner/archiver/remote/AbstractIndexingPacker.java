package org.vatplanner.archiver.remote;

import com.github.cliftonlabs.json_simple.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.vatplanner.archiver.RawDataFile;

public abstract class AbstractIndexingPacker implements Packer {

    private byte[] metaData;
    private final Map<RawDataFile, Integer> indexesByOriginal = new HashMap<>();

    private static final int FORMAT_VERSION = 1;
    private static final String CONTENT = "StatusDataFile";

    private int getFileIndex(RawDataFile original) {
        synchronized (indexesByOriginal) {
            return indexesByOriginal.computeIfAbsent(original, o -> indexesByOriginal.size() + 1);
        }
    }

    protected String getFileName(RawDataFile original) {
        return String.format("%08d.dat", getFileIndex(original));
    }

    protected byte[] getMetaData(Collection<RawDataFile> originals) {
        synchronized (this) {
            if (metaData != null) {
                return metaData;
            }

            metaData = encodeMetaData(originals);
        }

        return metaData;
    }

    private byte[] encodeMetaData(Collection<RawDataFile> originals) {
        JsonObject files = new JsonObject();
        for (RawDataFile original : originals) {
            files.put(getFileName(original), encodeFileMetaData(original));
        }

        JsonObject container = new JsonObject();
        container.put(RemoteMetaDataContainerJsonKeys.FORMAT_VERSION.getKey(), FORMAT_VERSION);
        container.put(RemoteMetaDataContainerJsonKeys.CONTENT.getKey(), CONTENT);
        container.put(RemoteMetaDataContainerJsonKeys.FILES.getKey(), files);

        return serializeJson(container);
    }

    private JsonObject encodeFileMetaData(RawDataFile original) {
        JsonObject file = new JsonObject();
        file.put(RemoteMetaDataFileJsonKeys.FETCH_TIME.getKey(), original.getFetchTime().toString());
        putFileMetaDataIfNotNull(file, RemoteMetaDataFileJsonKeys.FETCH_NODE, original.getFetchNode());
        putFileMetaDataIfNotNull(file, RemoteMetaDataFileJsonKeys.FETCH_URL_REQUESTED, original.getFetchUrlRequested());
        putFileMetaDataIfNotNull(file, RemoteMetaDataFileJsonKeys.FETCH_URL_RETRIEVED, original.getFetchUrlRetrieved());
        return file;
    }

    private void putFileMetaDataIfNotNull(JsonObject json, RemoteMetaDataFileJsonKeys key, Object value) {
        if (value == null) {
            return;
        }

        json.put(key.getKey(), value);
    }

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
