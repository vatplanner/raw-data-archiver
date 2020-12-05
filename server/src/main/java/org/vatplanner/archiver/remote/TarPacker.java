package org.vatplanner.archiver.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.vatplanner.archiver.common.RawDataFile;

/**
 * Packs data to a TAR archive with optional compression. See
 * {@link PackerMethods} for a discussion of compression algorithms.
 */
public class TarPacker extends AbstractIndexingPacker {

    private final CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();

    private final String compressionAlgorithm;

    /**
     * Creates a new packer for an uncompressed TAR archive.
     */
    public TarPacker() {
        this.compressionAlgorithm = null;
    }

    /**
     * Creates a new packer for a TAR file applying specified compression algorithm.
     * Compression is provided by Apache Commons Compress, see
     * {@link CompressorStreamFactory} for a list of available algorithms.
     *
     * @param compressionAlgorithm compression algorithm to apply, see
     *        {@link CompressorStreamFactory}; uncompressed if null
     * @throws IllegalArgumentException if selected algorithm is not supported for
     *         encoding
     */
    public TarPacker(String compressionAlgorithm) {
        if ((compressionAlgorithm != null)
            && !compressorStreamFactory.getOutputStreamCompressorNames().contains(compressionAlgorithm) //
        ) {
            throw new IllegalArgumentException("Unknown or unsupported compression algorithm: " + compressionAlgorithm);
        }

        this.compressionAlgorithm = compressionAlgorithm;
    }

    @Override
    public byte[] pack(Collection<RawDataFile> originals) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            OutputStream os = baos;
            if (compressionAlgorithm != null) {
                try {
                    os = compressorStreamFactory.createCompressorOutputStream(compressionAlgorithm, os);
                } catch (CompressorException ex) {
                    throw new IOException("Failed to create compressor output stream", ex);
                }
            }

            streamToTar(originals, os);

            baos.close();

            return baos.toByteArray();
        }
    }

    /**
     * Creates a new TAR archive stream on given {@link OutputStream} and writes all
     * data into the archive. Archive stream will be closed at the end of this
     * method.
     *
     * @param originals data to be archived
     * @param os underlying {@link OutputStream} to write archive into
     * @throws IOException
     */
    private void streamToTar(Collection<RawDataFile> originals, OutputStream os) throws IOException {
        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(os)) {
            // meta data should be available at start of stream
            writeEntry(taos, META_DATA_FILE_NAME, getMetaData(originals));

            // add all data file contents
            for (RawDataFile original : originals) {
                writeEntry(taos, getFileName(original), original.getData());
                original.clear();
            }

            taos.flush();
        }
    }

    /**
     * Writes a singly entry to the given archive stream.
     *
     * @param taos TAR stream to write to
     * @param fileName file name of this entry
     * @param data data contents of this entry
     * @throws IOException
     */
    private void writeEntry(TarArchiveOutputStream taos, String fileName, byte[] data) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(fileName);
        entry.setSize(data.length);
        taos.putArchiveEntry(entry);
        taos.write(data);
        taos.closeArchiveEntry();
    }

}
