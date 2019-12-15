package org.vatplanner.archiver.remote;

import com.github.cliftonlabs.json_simple.JsonKey;

public enum RemoteMetaDataFileJsonKeys implements JsonKey {
    FETCH_TIME("fetchTime"),
    FETCH_URL_REQUESTED("fetchUrlRequested"),
    FETCH_URL_RETRIEVED("fetchUrlRetrieved"),
    FETCH_NODE("fetchNode");

    private final String key;
    private final Object defaultValue;

    private RemoteMetaDataFileJsonKeys(String key) {
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
