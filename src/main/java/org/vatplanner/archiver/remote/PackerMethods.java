package org.vatplanner.archiver.remote;

/**
 * Methods by which data can be packed.
 * <p>
 * ZIP methods are useful to offer maximum compatibility with minimum or no
 * dependencies required to uncompress the result on a remote client.
 * </p>
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
     * {@link SingleThreadedZipDeflatePacker} for more information.
     */
    ZIP_DEFLATE_SINGLETHREADED,
    /**
     * Packs data to a "deflate" compressed ZIP file multi-threaded. Result will
     * be compressed to roughly 44% of original size. See
     * {@link MultiThreadedZipDeflatePacker} for more information.
     */
    ZIP_DEFLATE_MULTITHREADED;
}
