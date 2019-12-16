package org.vatplanner.archiver.remote;

import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.Collections;
import java.util.HashMap;

/**
 * {@link JsonKey}s to address generic (container) meta data in remote packed
 * files.
 */
public enum RemoteMetaDataContainerJsonKey implements JsonKey {
    /**
     * Version of packed data format. Only to be incremented if incompatible
     * with previous versions.
     */
    FORMAT_VERSION("formatVersion"),
    /**
     * Description of packed files. What's the content, how are the files to be
     * interpreted?
     */
    CONTENT("content"),
    /**
     * Meta data for all files in this container, indexed by file name used in
     * container.
     *
     * @see RemoteMetaDataFileJsonKeys
     */
    FILES("files", Collections.unmodifiableMap(new HashMap<String, JsonObject>()));

    private final String key;
    private final Object defaultValue;

    private RemoteMetaDataContainerJsonKey(String key) {
        this.key = key;
        this.defaultValue = null;
    }

    private RemoteMetaDataContainerJsonKey(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Object getValue() {
        return defaultValue;
    }

}
