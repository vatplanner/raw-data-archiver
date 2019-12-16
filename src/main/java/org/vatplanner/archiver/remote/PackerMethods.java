package org.vatplanner.archiver.remote;

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
public enum PackerMethods {
    /**
     * Packs data to an uncompressed ZIP file, should only be used if resulting
     * size is no concern. See {@link UncompressedZipPacker} for more
     * information.
     */
    ZIP_UNCOMPRESSED,
    /**
     * Packs data to a "deflate" compressed ZIP file single-threaded. Result
     * will be compressed to roughly 44% of original size. See
     * {@link SingleThreadedZipDeflatePacker} for more information. Unless ZIP
     * files are needed, {@link #TAR_GZIP} or {@link #TAR_BZIP2} are recommended
     * instead.
     */
    ZIP_DEFLATE_SINGLETHREADED,
    /**
     * Packs data to a "deflate" compressed ZIP file multi-threaded. Result will
     * be compressed to roughly 44% of original size. See
     * {@link MultiThreadedZipDeflatePacker} for more information.
     */
    ZIP_DEFLATE_MULTITHREADED,
    /**
     * Packs data to an uncompressed TAR file, should only be used if resulting
     * size is no concern.
     */
    TAR_UNCOMPRESSED,
    /**
     * Packs data to a DEFLATE compressed TAR file. Same efficiency (43% total
     * size) as {@link #ZIP_DEFLATE_SINGLETHREADED}, if result size is small use
     * {@link #ZIP_DEFLATE_MULTITHREADED} instead.
     */
    TAR_DEFLATE,
    /**
     * Packs data to a GZIP compressed TAR file. Same efficiency (43% total
     * size) as {@link #ZIP_DEFLATE_SINGLETHREADED}, if result size is small use
     * {@link #ZIP_DEFLATE_MULTITHREADED} instead.
     */
    TAR_GZIP,
    /**
     * Packs data to a BZIP2 compressed TAR file. Result will be compressed to
     * roughly 15% of original size but compression takes 3 times as long as
     * {@link #TAR_DEFLATE}, {@link #TAR_GZIP} or
     * {@link #ZIP_DEFLATE_SINGLETHREADED}.
     */
    TAR_BZIP2,
    /**
     * Packs data to a XZ compressed TAR file. While XZ (as well as LZMA)
     * compresses data to just 4% of its original size, encoding takes a very
     * long time (8 times as long as single-threaded DEFLATE or GZIP
     * compression). Using this algorithm to transport data is only feasible if
     * bandwidth or data volume is of very high concern.
     */
    TAR_XZ,
    /**
     * Packs data to a LZMA compressed TAR file. While LZMA (as well as XZ)
     * compresses data to just 4% of its original size, encoding takes a very
     * long time (8 times as long as single-threaded DEFLATE or GZIP
     * compression). Using this algorithm to transport data is only feasible if
     * bandwidth or data volume is of very high concern.
     */
    TAR_LZMA;
}
