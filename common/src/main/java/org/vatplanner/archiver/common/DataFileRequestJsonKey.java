package org.vatplanner.archiver.common;

import java.util.Collections;

import com.github.cliftonlabs.json_simple.JsonKey;

/**
 * JSON keys describing request parameters for data files.
 */
public enum DataFileRequestJsonKey implements JsonKey {
    DATA_FILE_FORMATS("dataFileFormats", Collections.emptyList()),
    PACKER_METHOD("packerMethod"),
    FILE_LIMIT("fileLimit", 1000),
    EARLIEST_FETCH_TIME("earliestFetchTime"),
    LATEST_FETCH_TIME("latestFetchTime");

    private final String key;
    private final Object defaultValue;

    private DataFileRequestJsonKey(String key) {
        this.key = key;
        this.defaultValue = null;
    }

    private DataFileRequestJsonKey(String key, Object defaultValue) {
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
