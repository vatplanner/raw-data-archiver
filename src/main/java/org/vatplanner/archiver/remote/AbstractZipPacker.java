package org.vatplanner.archiver.remote;

import java.io.IOException;
import java.util.Collection;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.vatplanner.archiver.RawDataFile;

public abstract class AbstractZipPacker extends AbstractIndexingPacker {

    protected final double expectedCompressionRatio;
    protected final int method;

    private ZipArchiveOutputStream zaos;
    private SeekableInMemoryByteChannel channel;

    public AbstractZipPacker(double expectedRatio, int method) {
        this.expectedCompressionRatio = expectedRatio;
        this.method = method;
    }

    protected ZipArchiveOutputStream createStream(Collection<RawDataFile> originals) throws IOException {
        int rawSize = originals.stream().mapToInt(f -> f.getData().length).sum();
        channel = new SeekableInMemoryByteChannel((int) (rawSize * expectedCompressionRatio));

        //System.out.println("created: " + ((int) (rawSize * expectedCompressionRatio)));
        zaos = new ZipArchiveOutputStream(channel);
        zaos.setMethod(method);
        return zaos;
    }

    protected byte[] closeStream() throws IOException {
        zaos.finish();
        zaos.flush();
        zaos.close();
        channel.close();
        byte[] compressed = channel.array();

        //System.out.println("written: " + zaos.getBytesWritten());
        //System.out.println("channel: " + compressed.length);
        return compressed;
        //return Arrays.copyOfRange(compressed, 0, (int) zaos.getBytesWritten());
        //return Arrays.copyOfRange(compressed, 0, (int) channel.size());
    }

    protected ZipArchiveEntry createContentEntry(RawDataFile original) {
        String name = getFileName(original);
        ZipArchiveEntry entry = new ZipArchiveEntry(name);
        entry.setMethod(method);

        byte[] data = original.getData();
        entry.setSize(data.length);

        return entry;
    }

    protected ZipArchiveEntry createMetaDataEntry(Collection<RawDataFile> originals) {
        ZipArchiveEntry entry = new ZipArchiveEntry("meta.json");
        entry.setMethod(method);

        byte[] data = getMetaData(originals);
        entry.setSize(data.length);

        return entry;
    }
}
