package org.vatplanner.archiver.remote;

import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.Collections;
import java.util.HashMap;

public enum RemoteMetaDataContainerJsonKeys implements JsonKey {
    FORMAT_VERSION("formatVersion"),
    CONTENT("content"),
    FILES("files", Collections.unmodifiableMap(new HashMap<String, JsonObject>()));

    private final String key;
    private final Object defaultValue;

    private RemoteMetaDataContainerJsonKeys(String key) {
        this.key = key;
        this.defaultValue = null;
    }

    private RemoteMetaDataContainerJsonKeys(String key, Object defaultValue) {
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
