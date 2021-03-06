package org.vatplanner.archiver.local;

import com.github.cliftonlabs.json_simple.JsonKey;

/**
 * Keys to reference information of an archive meta data JSON object stored on
 * file system.
 */
public enum LocalMetaDataJsonKey implements JsonKey {
    FETCH_TIME("timestamp"),
    FETCH_URL_REQUESTED("url");

    private final String key;
    private final Object defaultValue;

    private LocalMetaDataJsonKey(String key) {
        this.key = key;
        this.defaultValue = null;
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
