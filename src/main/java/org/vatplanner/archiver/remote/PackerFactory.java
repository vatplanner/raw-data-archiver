package org.vatplanner.archiver.remote;

public class PackerFactory {

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
