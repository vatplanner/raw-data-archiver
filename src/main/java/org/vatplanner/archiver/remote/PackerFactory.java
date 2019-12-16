package org.vatplanner.archiver.remote;

import org.apache.commons.compress.compressors.CompressorStreamFactory;

/**
 * Factory to more easily instantiate {@link Packer}s.
 */
public class PackerFactory {

    /**
     * Creates a new {@link Packer} to bundle files.
     *
     * @param method determines output format and handling
     * @return new packer instance implementing given method
     * @throws IllegalArgumentException if called with an unsupported method
     */
    public Packer createPacker(PackerMethods method) {
        switch (method) {
            case ZIP_UNCOMPRESSED:
                return new UncompressedZipPacker();

            case ZIP_DEFLATE_SINGLETHREADED:
                return new SingleThreadedZipDeflatePacker();

            case ZIP_DEFLATE_MULTITHREADED:
                return new MultiThreadedZipDeflatePacker();

            case TAR_UNCOMPRESSED:
                return new TarPacker();

            case TAR_BZIP2:
                return new TarPacker(CompressorStreamFactory.BZIP2);

            case TAR_GZIP:
                return new TarPacker(CompressorStreamFactory.GZIP);

            case TAR_XZ:
                return new TarPacker(CompressorStreamFactory.XZ);

            case TAR_DEFLATE:
                return new TarPacker(CompressorStreamFactory.DEFLATE);

            case TAR_LZMA:
                return new TarPacker(CompressorStreamFactory.LZMA);
        }

        throw new IllegalArgumentException("Unsupported packer method: " + method);
    }
}
