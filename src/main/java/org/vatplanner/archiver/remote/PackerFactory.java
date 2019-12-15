package org.vatplanner.archiver.remote;

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
        }

        throw new IllegalArgumentException("Unsupported packer method: " + method);
    }
}
