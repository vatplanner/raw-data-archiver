package org.vatplanner.archiver.client;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.RpcClient;
import com.rabbitmq.client.RpcClientParams;
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
import org.vatplanner.archiver.Configuration;
import org.vatplanner.archiver.DataFileRequestJsonKey;
import org.vatplanner.archiver.RawDataFile;
import org.vatplanner.archiver.camel.CamelConfiguration;
import org.vatplanner.archiver.remote.PackerMethod;
import org.vatplanner.archiver.remote.RemoteMetaDataContainerJsonKey;
import org.vatplanner.archiver.remote.RemoteMetaDataFileJsonKey;

public class RawDataFileClient {
    // FIXME: quick naive implementation for testing only, requires full overhaul

    private final Connection connection;
    private final Channel channel;
    private final String exchange;
    private final Duration timeout = Duration.ofMinutes(2);

    public RawDataFileClient() throws IOException, TimeoutException {
        // FIXME: use dedicated configuration
        // FIXME: do not throw raw exceptions
        CamelConfiguration camelConfig = new Configuration(null).getCamelConfig();

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(camelConfig.getAmqpHost());
        connectionFactory.setPort(camelConfig.getAmqpPort());
        connectionFactory.setUsername(camelConfig.getAmqpUsername());
        connectionFactory.setPassword(camelConfig.getAmqpPassword());
        connectionFactory.setVirtualHost(camelConfig.getAmqpVirtualHost());

        connection = connectionFactory.newConnection();

        exchange = camelConfig.getRequestsExchange();

        channel = connection.createChannel();
        channel.exchangeDeclarePassive(exchange); // passive: just check that the exchange exists
    }

    public CompletableFuture<Collection<RawDataFile>> request(PackerMethod packerMethod, Instant earliestFetchTime, Instant latestFetchTime, int fileLimit) {
        CompletableFuture<Collection<RawDataFile>> future = new CompletableFuture<>();

        // FIXME: extract class or method
        new Thread(() -> {
            try {
                RpcClient rpc = new RpcClient(new RpcClientParams()
                        .channel(channel)
                        .exchange(exchange)
                        .timeout((int) timeout.toMillis())
                        .routingKey("")
                        .useMandatory()
                );

                JsonObject jsonRequest = new JsonObject();
                jsonRequest
                        .putChain(DataFileRequestJsonKey.EARLIEST_FETCH_TIME.getKey(), earliestFetchTime.toString())
                        .putChain(DataFileRequestJsonKey.LATEST_FETCH_TIME.getKey(), latestFetchTime.toString())
                        .putChain(DataFileRequestJsonKey.PACKER_METHOD.getKey(), packerMethod.getRequestShortCode())
                        .putChain(DataFileRequestJsonKey.FILE_LIMIT.getKey(), fileLimit);

                System.out.println("sending request"); // DEBUG
                RpcClient.Response response = rpc.responseCall(jsonRequest.toJson().getBytes());

                String responsePackerMethodString = (String) response.getProperties().getHeaders().getOrDefault("packerMethod", packerMethod.getPackedShortCode());
                byte[] responseBody = response.getBody();
                System.out.println("response " + responsePackerMethodString + " " + responseBody.length);  // DEBUG
                BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(responseBody));
                responseBody = null; // FIXME: do not save byte[] to variable, just forward to BAIS if debug output no longer needed

                rpc.close();
                channel.close();
                connection.close();

                System.out.println("----"); // DEBUG

                Map<String, RawDataFile> rawDataFiles = new HashMap<>();

                // FIXME: resolve implementations from server-submitted header
                try {
                    CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(bis);
                    bis = new BufferedInputStream(cis);
                } catch (Exception ex) {
                    // expected for ZIP
                    ex.printStackTrace();
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

                    System.out.println(name);

                    if (name.equals("meta.json")) { // FIXME: use common constant
                        InputStreamReader dataReader = new InputStreamReader(new ByteArrayInputStream(data));
                        JsonObject meta = (JsonObject) Jsoner.deserialize(dataReader);
                        dataReader.close();

                        Map<String, JsonObject> fileMetas = meta.getMapOrDefault(RemoteMetaDataContainerJsonKey.FILES);
                        for (Map.Entry<String, JsonObject> fileMeta : fileMetas.entrySet()) {
                            String fileName = fileMeta.getKey();
                            RawDataFile rawDataFile = rawDataFiles.computeIfAbsent(fileName, n -> new RawDataFile(null));
                            rawDataFile.setFetchTime(Instant.parse(fileMeta.getValue().getStringOrDefault(RemoteMetaDataFileJsonKey.FETCH_TIME)));
                            rawDataFile.setFetchNode(fileMeta.getValue().getStringOrDefault(RemoteMetaDataFileJsonKey.FETCH_NODE));
                            rawDataFile.setFetchUrlRequested(fileMeta.getValue().getStringOrDefault(RemoteMetaDataFileJsonKey.FETCH_URL_REQUESTED));
                            rawDataFile.setFetchUrlRetrieved(fileMeta.getValue().getStringOrDefault(RemoteMetaDataFileJsonKey.FETCH_URL_RETRIEVED));
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
