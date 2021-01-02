package org.vatplanner.archiver.camel;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.common.DataFileRequestJsonKey;
import org.vatplanner.archiver.common.PackerMethod;
import org.vatplanner.archiver.common.RawDataFile;
import org.vatplanner.archiver.local.Loader;
import org.vatplanner.archiver.remote.Packer;
import org.vatplanner.archiver.remote.PackerFactory;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * Processes requests for raw data files.
 */
public class DataFileRequestProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataFileRequestProcessor.class);

    private final Loader loader;
    private final PackerFactory packerFactory;

    public DataFileRequestProcessor(Loader loader, PackerFactory packerFactory) {
        this.loader = loader;
        this.packerFactory = packerFactory;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // get request body
        Message in = exchange.getIn();
        String body = (String) in.getBody(String.class);
        JsonObject json = Jsoner.deserialize(body, new JsonObject());

        // read request configuration
        PackerMethod packerMethod = PackerMethod.byRequestShortCode(
            json.getString(DataFileRequestJsonKey.PACKER_METHOD) //
        );
        Instant earliestFetchTime = Instant.parse(json.getString(DataFileRequestJsonKey.EARLIEST_FETCH_TIME));
        Instant latestFetchTime = Instant.parse(json.getString(DataFileRequestJsonKey.LATEST_FETCH_TIME));
        int fileLimit = json.getIntegerOrDefault(DataFileRequestJsonKey.FILE_LIMIT);
        Set<String> wantedDataFileFormats = new HashSet<String>(
            json.getCollectionOrDefault(DataFileRequestJsonKey.DATA_FILE_FORMATS) //
        );

        LOGGER.info(
            "Processing data file request: earliest {}, latest {}, packer {}, file limit {}, data file formats {}",
            earliestFetchTime, latestFetchTime, packerMethod, fileLimit, wantedDataFileFormats //
        );

        // load data
        Instant beforeLoading = Instant.now();
        List<RawDataFile> loaded = loader.load(
            earliestFetchTime,
            latestFetchTime,
            fileLimit,
            wantedDataFileFormats //
        );
        Instant afterLoading = Instant.now();

        // pack result
        Instant beforePacking = Instant.now();
        Packer packer = packerFactory.createPacker(packerMethod);
        byte[] packed = packer.pack(loaded);
        Instant afterPacking = Instant.now();

        LOGGER.info(
            "Finished data file request: earliest {}, latest {}, packer {}, file limit {} [{} files, loaded {}ms, packed {}ms, total {}ms, size {}kB]",
            earliestFetchTime, latestFetchTime, packerMethod, fileLimit,
            loaded.size(),
            Duration.between(beforeLoading, afterLoading).toMillis(),
            Duration.between(beforePacking, afterPacking).toMillis(),
            Duration.between(beforeLoading, afterPacking).toMillis(),
            packed.length / 1024 //
        );

        // assemble response message
        Message out = exchange.getIn().copy();
        out.setBody(packed);
        exchange.setMessage(out);

        // TODO: indicate packed method in message header
    }

}
