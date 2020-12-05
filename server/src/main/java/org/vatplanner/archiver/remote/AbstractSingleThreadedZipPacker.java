package org.vatplanner.archiver.remote;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.vatplanner.archiver.common.RawDataFile;

/**
 * Base class to pack data to a ZIP file single-threaded. Actually, this base
 * class does all the work... all that remains for implementing classes is to
 * provide a configuration.
 */
public abstract class AbstractSingleThreadedZipPacker extends AbstractZipPacker {

    /**
     * Configures expected total file size ratio and storage method. The ratio will
     * be used to pre-allocate memory to write the resulting ZIP file to. It is
     * important to provide a slightly higher estimation than actually expected
     * since resizing the underlying in-memory implementation
     * {@link SeekableInMemoryByteChannel} has a heavy performance impact and should
     * be avoided if many files are to be added (as is the case in this
     * application).
     *
     * @param expectedRatio expected ratio of output size compared to input; should
     *        be higher than actual
     * @param method method to store files with, see {@link ZipArchiveEntry}
     */
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
