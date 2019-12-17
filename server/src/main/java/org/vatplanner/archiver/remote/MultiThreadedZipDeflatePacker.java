package org.vatplanner.archiver.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.vatplanner.archiver.common.RawDataFile;

/**
 * Packs all data to a ZIP file using "deflate" compression using multiple
 * threads. While ZIP files are the most universally accessible way of packing
 * data, the result of multi-threaded "deflate" is only compressed to a factor
 * of roughly 0.44.
 *
 * <p>
 * See {@link SingleThreadedZipDeflatePacker} if single-threaded compression is
 * needed.
 * </p>
 *
 * @see SingleThreadZipDeflatePacker
 */
public class MultiThreadedZipDeflatePacker extends AbstractZipPacker {

    public MultiThreadedZipDeflatePacker() {
        super(0.5, ZipEntry.DEFLATED);
    }

    @Override
    public byte[] pack(Collection<RawDataFile> originals) throws IOException {
        ParallelScatterZipCreator zipCreator = new ParallelScatterZipCreator();

        ZipArchiveOutputStream zaos = createStream(originals);

        // store meta data
        zipCreator.addArchiveEntry(() -> {
            ZipArchiveEntry metaDataEntry = createMetaDataEntry(originals);
            byte[] data = getMetaData(originals);
            return ZipArchiveEntryRequest.createZipArchiveEntryRequest(metaDataEntry, () -> new ByteArrayInputStream(data));
        });

        // store all content
        for (RawDataFile original : originals) {
            zipCreator.addArchiveEntry(() -> {
                ZipArchiveEntry entry = createContentEntry(original);
                byte[] data = original.getData();

                original.clear();

                return ZipArchiveEntryRequest.createZipArchiveEntryRequest(entry, () -> new ByteArrayInputStream(data));
            });
        }

        try {
            zipCreator.writeTo(zaos);
        } catch (InterruptedException | ExecutionException ex) {
            throw new IOException("Parallel execution failed", ex);
        }

        return closeStream();
    }
}
