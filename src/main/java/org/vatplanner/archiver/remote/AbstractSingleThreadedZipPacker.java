package org.vatplanner.archiver.remote;

import java.io.IOException;
import java.util.Collection;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.vatplanner.archiver.RawDataFile;

public abstract class AbstractSingleThreadedZipPacker extends AbstractZipPacker {

    public AbstractSingleThreadedZipPacker(double expectedRatio, int method) {
        super(expectedRatio, method);
    }

    @Override
    public byte[] pack(Collection<RawDataFile> originals) throws IOException {
        ZipArchiveOutputStream zaos = createStream(originals);

        // store meta data
        ZipArchiveEntry metaDataEntry = createMetaDataEntry(originals);
        zaos.putArchiveEntry(metaDataEntry);
        zaos.write(getMetaData(originals));
        zaos.closeArchiveEntry();

        // store all content
        for (RawDataFile original : originals) {
            ZipArchiveEntry contentEntry = createContentEntry(original);
            zaos.putArchiveEntry(contentEntry);
            zaos.write(original.getData());
            zaos.closeArchiveEntry();

            original.clear();
        }

        return closeStream();
    }
}
