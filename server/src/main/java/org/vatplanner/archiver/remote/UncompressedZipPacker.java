package org.vatplanner.archiver.remote;

import java.util.zip.ZipEntry;

/**
 * Packs all data to a universally accessible but uncompressed ZIP file. This
 * implementation comes in handy if data size is of no concern and just fast
 * packing to a universal container is wanted.
 */
public class UncompressedZipPacker extends AbstractSingleThreadedZipPacker {

    public UncompressedZipPacker() {
        super(1.1, ZipEntry.STORED);
    }

}
