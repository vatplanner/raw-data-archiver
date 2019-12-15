package org.vatplanner.archiver.remote;

import java.util.zip.ZipEntry;

/**
 * Packs all data to a ZIP file using "deflate" compression using just a single
 * thread. While ZIP files are the most universally accessible way of packing
 * data, the result can only be compressed to a factor of 0.35 with medium
 * amount of time required for compression.
 *
 * @see {@link MultithreadedZipDeflatePacker} for a multi-threaded
 * implementation
 */
public class SingleThreadedZipDeflatePacker extends AbstractSingleThreadedZipPacker {

    public SingleThreadedZipDeflatePacker() {
        super(0.5, ZipEntry.DEFLATED);
    }
}
