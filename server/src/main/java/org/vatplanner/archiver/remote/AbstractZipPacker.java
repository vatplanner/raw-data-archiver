package org.vatplanner.archiver.remote;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.vatplanner.archiver.common.RawDataFile;

/**
 * Base class to pack data to a ZIP file. General handling in terms of container
 * preparation and finishing is the same for single-threaded and multi-threaded
 * processing, so it is provided by this class. APIs to add actual files
 * differs, so that's left to implementing classes.
 *
 * @see AbstractSingleThreadedZipPacker
 * @see MultiThreadedZipDeflatePacker
 */
public abstract class AbstractZipPacker extends AbstractIndexingPacker {

    protected final double expectedCompressionRatio;
    protected final int method;

    private ZipArchiveOutputStream zaos;
    private SeekableInMemoryByteChannel channel;

    /**
     * Configures expected total file size ratio and storage method. The ratio
     * will be used to pre-allocate memory to write the resulting ZIP file to.
     * It is important to provide a slightly higher estimation than actually
     * expected since resizing the underlying in-memory implementation
     * {@link SeekableInMemoryByteChannel} has a heavy performance impact and
     * should be avoided if many files are to be added (as is the case in this
     * application).
     *
     * @param expectedRatio expected ratio of output size compared to input;
     * should be higher than actual
     * @param method method to store files with, see {@link ZipArchiveEntry}
     */
    public AbstractZipPacker(double expectedRatio, int method) {
        this.expectedCompressionRatio = expectedRatio;
        this.method = method;
    }

    /**
     * Starts a new ZIP stream configured to use requested storage method and
     * creates a new in-memory channel to store the result in.
     *
     * @param originals files to be stored; used to calculate required memory
     * size
     * @return open ZIP stream ready to encode data
     * @throws IOException
     */
    protected ZipArchiveOutputStream createStream(Collection<RawDataFile> originals) throws IOException {
        int rawSize = originals.stream().mapToInt(f -> f.getData().length).sum();
        channel = new SeekableInMemoryByteChannel((int) (rawSize * expectedCompressionRatio));

        zaos = new ZipArchiveOutputStream(channel);
        zaos.setMethod(method);
        return zaos;
    }

    /**
     * Closes the ZIP stream and returns the result as a byte array.
     *
     * @return resulting ZIP file as a byte array
     * @throws IOException
     */
    protected byte[] closeStream() throws IOException {
        zaos.finish();
        zaos.flush();
        zaos.close();
        channel.close();
        byte[] compressed = channel.array();

        return Arrays.copyOfRange(compressed, 0, (int) channel.position());
    }

    /**
     * Creates a new archive entry to store the given file. This only sets and
     * provides archive meta data required to index data within the ZIP file.
     * Actual data needs to be written separate from this method call; how that
     * is to be done depends on the specific API.
     *
     * @param original original file to create archive entry for
     * @return archive entry for given file
     */
    protected ZipArchiveEntry createContentEntry(RawDataFile original) {
        String name = getFileName(original);
        ZipArchiveEntry entry = new ZipArchiveEntry(name);
        entry.setMethod(method);

        byte[] data = original.getData();
        entry.setSize(data.length);

        return entry;
    }

    /**
     * Creates the archive entry to address the application JSON meta data file.
     * This only sets and provides archive meta data required to index data
     * within the ZIP file. Actual data needs to be written separate from this
     * method call; how that is to be done depends on the specific API.
     *
     * <p>
     * No further files must be indexed after calling this method as it
     * finalizes the JSON meta data; see {@link AbstractIndexingPacker} for
     * details.
     * </p>
     *
     * @param originals all data files being stored in the ZIP file
     * @return archive entry for the application JSON meta data file
     */
    protected ZipArchiveEntry createMetaDataEntry(Collection<RawDataFile> originals) {
        ZipArchiveEntry entry = new ZipArchiveEntry(META_DATA_FILE_NAME);
        entry.setMethod(method);

        byte[] data = getMetaData(originals);
        entry.setSize(data.length);

        return entry;
    }
}
