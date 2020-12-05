package org.vatplanner.archiver.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Methods by which data can be packed.
 * <p>
 * ZIP methods are useful to offer maximum compatibility with minimum or no
 * dependencies required to uncompress the result on a remote client. Files can
 * be accessed randomly, which might be useful for some clients. Encoding ZIP
 * files requires an estimated result size to be pre-allocated in memory.
 * </p>
 * <p>
 * TAR methods result in a continuous data stream. This means clients need to
 * read the full file in one go. Multiple compression algorithms are available
 * with trade-offs between speed and efficiency:
 * </p>
 * <ul>
 * <li>{@link #TAR_UNCOMPRESSED} is equal to {@link #ZIP_UNCOMPRESSED} and
 * should be used when bandwidth is no concern and an (almost) immediate result
 * is wanted.</li>
 * <li>{@link #TAR_DEFLATE} and {@link #TAR_GZIP} result in 43% of the original
 * file size, working single-threaded. "Deflate" algorithm with same efficiency
 * is also used by ZIP files but ZIP compression offers a multi-threaded
 * implementation. If it does not matter if the result is encoded as TAR or ZIP,
 * {@link #ZIP_DEFLATE_MULTITHREADED} is recommended over TAR with deflate/gzip.
 * </li>
 * <li>{@link #TAR_BZIP2} results in 15% of the original file size but takes 3
 * times as long to encode compared to {@link #TAR_DEFLATE} or
 * {@link #TAR_GZIP}.</li>
 * <li>{@link #TAR_XZ} and {@link #TAR_LZMA} result in just 4% of the original
 * file size but take 8 times as long to encode compared to {@link #TAR_DEFLATE}
 * or {@link #TAR_GZIP}. As replies will be hugely delayed due to compression,
 * these algorithms should only be chosen if network bandwidth is very low or
 * data volume has to be conserved.</li>
 * </ul>
 */
public enum PackerMethod {
    /**
     * Packs data to an uncompressed ZIP file, should only be used if resulting size
     * is no concern. See {@link UncompressedZipPacker} for more information.
     */
    ZIP_UNCOMPRESSED("zip/uncompressed"),

    /**
     * Packs data to a "deflate" compressed ZIP file. Result will be compressed to
     * roughly 44% of original size. Server decides whether result is encoded
     * single- or multi-threaded.
     */
    ZIP_DEFLATE("zip/deflate", "zip/deflate"),

    /**
     * Packs data to a "deflate" compressed ZIP file single-threaded. Result will be
     * compressed to roughly 44% of original size. See
     * {@link SingleThreadedZipDeflatePacker} for more information. Unavailable to
     * client-side requests, use {@link #ZIP_DEFLATE} instead.
     */
    ZIP_DEFLATE_SINGLETHREADED(null, ZIP_DEFLATE.packedShortCode),

    /**
     * Packs data to a "deflate" compressed ZIP file multi-threaded. Result will be
     * compressed to roughly 44% of original size. See
     * {@link MultiThreadedZipDeflatePacker} for more information. Unavailable to
     * client-side requests, use {@link #ZIP_DEFLATE} instead.
     */
    ZIP_DEFLATE_MULTITHREADED(null, ZIP_DEFLATE.packedShortCode),

    /**
     * Packs data to an uncompressed TAR file, should only be used if resulting size
     * is no concern.
     */
    TAR_UNCOMPRESSED("tar"),

    /**
     * Packs data to a DEFLATE compressed TAR file. Same efficiency (43% total size)
     * as {@link #ZIP_DEFLATE_SINGLETHREADED}, if result size is small use
     * {@link #ZIP_DEFLATE_MULTITHREADED} instead.
     */
    TAR_DEFLATE("tar+deflate"),

    /**
     * Packs data to a GZIP compressed TAR file. Same efficiency (43% total size) as
     * {@link #ZIP_DEFLATE_SINGLETHREADED}, if result size is small use
     * {@link #ZIP_DEFLATE_MULTITHREADED} instead.
     */
    TAR_GZIP("tar+gzip"),

    /**
     * Packs data to a BZIP2 compressed TAR file. Result will be compressed to
     * roughly 15% of original size but compression takes 3 times as long as
     * {@link #TAR_DEFLATE}, {@link #TAR_GZIP} or
     * {@link #ZIP_DEFLATE_SINGLETHREADED}.
     */
    TAR_BZIP2("tar+bzip2"),

    /**
     * Packs data to a XZ compressed TAR file. While XZ (as well as LZMA) compresses
     * data to just 4% of its original size, encoding takes a very long time (8
     * times as long as single-threaded DEFLATE or GZIP compression). Using this
     * algorithm to transport data is only feasible if bandwidth or data volume is
     * of very high concern.
     */
    TAR_XZ("tar+xz"),

    /**
     * Packs data to a LZMA compressed TAR file. While LZMA (as well as XZ)
     * compresses data to just 4% of its original size, encoding takes a very long
     * time (8 times as long as single-threaded DEFLATE or GZIP compression). Using
     * this algorithm to transport data is only feasible if bandwidth or data volume
     * is of very high concern.
     */
    TAR_LZMA("tar+lzma");

    private final String requestShortCode;
    private final String packedShortCode;

    private static final Map<String, PackerMethod> BY_REQUEST_SHORT_CODE = new HashMap<>();
    private static final Map<String, PackerMethod> BY_PACKED_SHORT_CODE = new HashMap<>();

    static {
        for (PackerMethod method : values()) {
            BY_REQUEST_SHORT_CODE.put(method.requestShortCode, method);

            if (!method.aliasesOtherPackedShortCode()) {
                BY_PACKED_SHORT_CODE.put(method.packedShortCode, method);
            }
        }
    }

    private PackerMethod(String packedShortCode) {
        this.requestShortCode = packedShortCode;
        this.packedShortCode = packedShortCode;
    }

    private PackerMethod(String requestShortCode, String packedShortCode) {
        this.requestShortCode = requestShortCode;
        this.packedShortCode = packedShortCode;
    }

    /**
     * Returns the short code used to identify the method after packing.
     *
     * @return short code identifying method after packing
     */
    public String getPackedShortCode() {
        return packedShortCode;
    }

    /**
     * Returns the short code used to identify the method on requests.
     *
     * @return short code identifying method on requests
     */
    public String getRequestShortCode() {
        return requestShortCode;
    }

    /**
     * Resolves the given short code used during requests to a method.
     *
     * @param requestShortCode short code identifying requested method
     * @return matching method
     * @throws IllegalArgumentException if short code is unknown or null
     */
    public static PackerMethod byRequestShortCode(String requestShortCode) {
        if (requestShortCode == null) {
            throw new IllegalArgumentException("request short code must not be null");
        }

        PackerMethod method = BY_REQUEST_SHORT_CODE.get(requestShortCode);
        if (method == null) {
            throw new IllegalArgumentException("unknown request short code \"" + requestShortCode + "\"");
        }

        return method;
    }

    /**
     * Resolves the given short code used to identify a method used for packing.
     *
     * @param packedShortCode short code identifying method used for packing
     * @return matching method
     * @throws IllegalArgumentException if short code is unknown or null
     */
    public static PackerMethod byPackedShortCode(String packedShortCode) {
        if (packedShortCode == null) {
            throw new IllegalArgumentException("packed short code must not be null");
        }

        PackerMethod method = BY_PACKED_SHORT_CODE.get(packedShortCode);
        if (method == null) {
            throw new IllegalArgumentException("unknown packed short code \"" + packedShortCode + "\"");
        }

        return method;
    }

    /**
     * Checks if this is a ZIP method.
     *
     * @return true if ZIP method, false if not
     */
    public boolean isZipMethod() {
        return (this == ZIP_UNCOMPRESSED)
            || (this == ZIP_DEFLATE)
            || (this == ZIP_DEFLATE_SINGLETHREADED)
            || (this == ZIP_DEFLATE_MULTITHREADED);
    }

    /**
     * Checks if this method yields an uncompressed result.
     *
     * @return true if uncompressed, false if compressed
     */
    public boolean isUncompressed() {
        return (this == ZIP_UNCOMPRESSED) || (this == TAR_UNCOMPRESSED);
    }

    /**
     * Checks if this method aliases another method's packed short code.
     *
     * @return true if aliasing, false if not
     */
    private boolean aliasesOtherPackedShortCode() {
        return (this == ZIP_DEFLATE_SINGLETHREADED) || (this == ZIP_DEFLATE_MULTITHREADED);
    }

}
