package org.vatplanner.archiver;

import java.time.Instant;

/**
 * Holds all information readable for an archived data file.
 *
 * <p>
 * {@link #clear()} shall be called as soon as data is no longer needed to
 * conserve memory. {@link #getData()} and {@link #setData(byte[])} will fail
 * afterwards.
 * </p>
 */
public class RawDataFile {

    private Instant fetchTime;
    private String fetchUrlRequested;
    private String fetchUrlRetrieved;
    private String fetchNode;
    private byte[] data;
    private boolean isCleared = false;

    public RawDataFile(Instant fetchTime) {
        this.fetchTime = fetchTime;
    }

    /**
     * Returns the identification of the node who has fetched the data.
     *
     * @return identification of the node who has fetched the data
     */
    public String getFetchNode() {
        return fetchNode;
    }

    public RawDataFile setFetchNode(String fetchNode) {
        this.fetchNode = fetchNode;
        return this;
    }

    /**
     * Returns the timestamp of when fetched data had been requested.
     *
     * @return timestamp of when fetched data had been requested
     */
    public Instant getFetchTime() {
        return fetchTime;
    }

    public RawDataFile setFetchTime(Instant fetchTime) {
        this.fetchTime = fetchTime;
        return this;
    }

    /**
     * Returns the original URL requested to retrieve fetched data from (before
     * following redirects).
     *
     * @return original URL requested to retrieve fetched data from
     */
    public String getFetchUrlRequested() {
        return fetchUrlRequested;
    }

    public RawDataFile setFetchUrlRequested(String fetchUrlRequested) {
        this.fetchUrlRequested = fetchUrlRequested;
        return this;
    }

    /**
     * Returns the actual URL where fetched data had been retrieved from (after
     * following redirects).
     *
     * @return actual URL where fetched data had been retrieved from
     */
    public String getFetchUrlRetrieved() {
        return fetchUrlRetrieved;
    }

    public RawDataFile setFetchUrlRetrieved(String fetchUrlRetrieved) {
        this.fetchUrlRetrieved = fetchUrlRetrieved;
        return this;
    }

    /**
     * Returns the fetched data.
     *
     * @return fetched data
     * @throws RuntimeException if {@link #clear()} has been called to evict
     * data from memory
     */
    public byte[] getData() {
        if (isCleared) {
            throw new RuntimeException("attempted access to explicitely evicted byte array");
        }

        return data;
    }

    public RawDataFile setData(byte[] data) {
        if (isCleared) {
            throw new RuntimeException("recycling partially evicted instances seems like an error and thus is not allowed");
        }

        this.data = data;

        return this;
    }

    /**
     * Marks fetched data to be cleared from memory.
     */
    public void clear() {
        data = null;
        isCleared = true;
    }
}
