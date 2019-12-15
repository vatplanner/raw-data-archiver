package org.vatplanner.archiver.remote;

import com.github.cliftonlabs.json_simple.JsonKey;
import org.vatplanner.archiver.RawDataFile;

/**
 * {@link JsonKey}s to address meta data for a single file in remote packed
 * container.
 */
public enum RemoteMetaDataFileJsonKeys implements JsonKey {
    /**
     * Holds the timestamp of when fetched data had been requested.
     *
     * @see RawDataFile#getFetchTime()
     */
    FETCH_TIME("fetchTime"),
    /**
     * Holds the original URL requested to retrieve fetched data from (before
     * following redirects).
     *
     * @see RawDataFile#getFetchUrlRequested()
     */
    FETCH_URL_REQUESTED("fetchUrlRequested"),
    /**
     * Holds the actual URL where fetched data had been retrieved from (after
     * following redirects).
     *
     * @see RawDataFile#getFetchUrlRetrieved()
     */
    FETCH_URL_RETRIEVED("fetchUrlRetrieved"),
    /**
     * Holds the identification of the node who has fetched the data.
     *
     * @see RawDataFile#getFetchNode()
     */
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
