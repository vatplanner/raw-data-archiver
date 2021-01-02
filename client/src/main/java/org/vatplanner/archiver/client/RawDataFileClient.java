package org.vatplanner.archiver.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.archiver.common.DataFileRequestJsonKey;
import org.vatplanner.archiver.common.PackerMethod;
import org.vatplanner.archiver.common.RawDataFile;
import org.vatplanner.archiver.common.RemoteMetaDataContainerJsonKey;
import org.vatplanner.archiver.common.RemoteMetaDataFileJsonKey;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.RpcClient;
import com.rabbitmq.client.RpcClientParams;

public class RawDataFileClient {
    // FIXME: quick naive implementation for testing only, requires full overhaul

    private static final Logger LOGGER = LoggerFactory.getLogger(RawDataFile.class);

    private final Connection connection;
    private final String exchange;
    private final Duration timeout = Duration.ofMinutes(2); // FIXME: make config option

    public RawDataFileClient(ClientConfiguration config) throws IOException, TimeoutException {
        // FIXME: use dedicated configuration
        // FIXME: do not throw raw exceptions

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(config.getAmqpHost());
        connectionFactory.setPort(config.getAmqpPort());
        connectionFactory.setUsername(config.getAmqpUsername());
        connectionFactory.setPassword(config.getAmqpPassword());
        connectionFactory.setVirtualHost(config.getAmqpVirtualHost());

        connection = connectionFactory.newConnection();

        exchange = config.getRequestsExchange();
    }

    public CompletableFuture<Collection<RawDataFile>> request(PackerMethod packerMethod, Instant earliestFetchTime, Instant latestFetchTime, int fileLimit) {
        CompletableFuture<Collection<RawDataFile>> future = new CompletableFuture<>();

        final Channel channel;
        try {
            channel = connection.createChannel();
            channel.exchangeDeclarePassive(exchange); // passive: just check that the exchange exists
        } catch (IOException ex) {
            LOGGER.warn("Failed to open AMQP channel.", ex);
            future.completeExceptionally(ex);
            return future;
        }

        // FIXME: extract class or method
        new Thread(() -> {
            try {
                RpcClient rpc = new RpcClient(
                    new RpcClientParams()
                        .channel(channel)
                        .exchange(exchange)
                        .timeout((int) timeout.toMillis())
                        .routingKey("")
                        .useMandatory() //
                );

                JsonObject jsonRequest = new JsonObject();
                jsonRequest
                    .putChain(DataFileRequestJsonKey.EARLIEST_FETCH_TIME.getKey(), earliestFetchTime.toString())
                    .putChain(DataFileRequestJsonKey.LATEST_FETCH_TIME.getKey(), latestFetchTime.toString())
                    .putChain(DataFileRequestJsonKey.PACKER_METHOD.getKey(), packerMethod.getRequestShortCode())
                    .putChain(DataFileRequestJsonKey.FILE_LIMIT.getKey(), fileLimit);

                String jsonRequestString = jsonRequest.toJson();
                LOGGER.debug("sending RPC request to AMQP: {}", jsonRequestString);
                RpcClient.Response response = rpc.responseCall(jsonRequestString.getBytes());

                // FIXME: use server-submitted header for packer method
                String responsePackerMethodString = (String) response.getProperties()
                    .getHeaders()
                    .getOrDefault("packerMethod", packerMethod.getPackedShortCode());
                byte[] responseBody = response.getBody();
                LOGGER.debug(
                    "AMQP RPC response arrived, packer method {}, encoded length {}",
                    responsePackerMethodString,
                    responseBody.length //
                );
                PackerMethod responsePackerMethod = PackerMethod.byPackedShortCode(responsePackerMethodString);
                BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(responseBody));
                /*
                 * FIXME: do not save byte[] to variable, just forward to BAIS if debug output
                 * no longer needed
                 */
                responseBody = null;

                rpc.close();
                channel.close();

                Map<String, RawDataFile> rawDataFiles = new HashMap<>();

                boolean needsExplicitDecompression = //
                    !(responsePackerMethod.isUncompressed() || responsePackerMethod.isZipMethod());
                if (needsExplicitDecompression) {
                    try {
                        CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(bis);
                        bis = new BufferedInputStream(cis);
                    } catch (Exception ex) {
                        throw new RuntimeException(
                            "response packer method "
                                + responsePackerMethod
                                + " requires explicit decompression but setting up stream failed",
                            ex //
                        );
                    }
                }
                ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(bis);

                ArchiveEntry entry;
                while ((entry = ais.getNextEntry()) != null) {
                    String name = entry.getName();

                    int size = (int) entry.getSize();
                    byte[] data = new byte[size];
                    int read = 0;
                    while (read < size) {
                        read += ais.read(data, read, size - read);
                    }

                    LOGGER.trace("reading {}", name);

                    if (name.equals("meta.json")) { // FIXME: use common constant
                        InputStreamReader dataReader = new InputStreamReader(new ByteArrayInputStream(data));
                        JsonObject meta = (JsonObject) Jsoner.deserialize(dataReader);
                        dataReader.close();

                        Map<String, JsonObject> fileMetas = meta.getMapOrDefault(RemoteMetaDataContainerJsonKey.FILES);
                        for (Map.Entry<String, JsonObject> fileMeta : fileMetas.entrySet()) {
                            String fileName = fileMeta.getKey();
                            JsonObject fields = fileMeta.getValue();
                            RawDataFile rawDataFile = rawDataFiles.computeIfAbsent(
                                fileName,
                                n -> new RawDataFile(null) //
                            );
                            rawDataFile.setFormatName(
                                fields.getString(RemoteMetaDataFileJsonKey.FORMAT_NAME) //
                            );
                            rawDataFile.setFetchTime(Instant.parse(
                                fields.getStringOrDefault(RemoteMetaDataFileJsonKey.FETCH_TIME) //
                            ));
                            rawDataFile.setFetchNode(
                                fields.getStringOrDefault(RemoteMetaDataFileJsonKey.FETCH_NODE) //
                            );
                            rawDataFile.setFetchUrlRequested(
                                fields.getStringOrDefault(RemoteMetaDataFileJsonKey.FETCH_URL_REQUESTED) //
                            );
                            rawDataFile.setFetchUrlRetrieved(
                                fields.getStringOrDefault(RemoteMetaDataFileJsonKey.FETCH_URL_RETRIEVED) //
                            );
                        }
                    } else {
                        RawDataFile rawDataFile = rawDataFiles.computeIfAbsent(name, n -> new RawDataFile(null));
                        rawDataFile.setData(data);
                    }
                }

                ais.close();
                bis.close();

                future.complete(rawDataFiles.values());
            } catch (Exception ex) {
                // FIXME: close resources
                future.completeExceptionally(ex);
            }
        }).start();

        return future;
    }

}
