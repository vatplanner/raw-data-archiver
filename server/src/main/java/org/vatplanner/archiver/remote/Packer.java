package org.vatplanner.archiver.remote;

import java.io.IOException;
import java.util.Collection;
import org.vatplanner.archiver.common.RawDataFile;

/**
 * Packs data into a single file for bundled transmission of multiple
 * {@link RawDataFile}s holding both content and meta-data. Output can be
 * compressed or remain uncompressed but all implementations need to hold full
 * data at the end.
 */
public interface Packer {

    /**
     * Pack all given original data files to a single binary bundle.
     *
     * @param originals data to be packed
     * @return packed data
     * @throws IOException
     */
    public byte[] pack(Collection<RawDataFile> originals) throws IOException;
}
