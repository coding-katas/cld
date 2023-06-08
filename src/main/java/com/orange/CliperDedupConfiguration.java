package com.orange;

import org.springframework.beans.factory.annotation.Value;
import org.apache.kafka.streams.StreamsConfig;
import com.orange.processor.DedupTopology;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.util.Properties;
import com.orange.dto.CliperDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class CliperDedupConfiguration {
    private DedupTopology dedupTopology;

    @Value(value = "${spring.kafka.bootstrap-servers:instance-1:9093}")
    private String bootstrapServers;

    @Value(value = "${spring.kafka.security.protocol:SSL}")
    private String securityProtocol;

    @Value(value = "${spring.kafka.ssl.trust-store-location:/data/certs/truststore.jks}")
    private String trustStoreLocation;

    @Value(value = "${spring.kafka.ssl.trust-store-password:password}")
    private String trustStorePassword;

    @Value(value = "${spring.kafka.ssl.key-store-location:/data/certs/keystore.jks}")
    private String keyStoreLocation;

    @Value(value = "${spring.kafka.ssl.key-store-password:password}")
    private String keyStorePassword;

    @Value(value = "${spring.kafka.ssl.key-password:password}")
    private String keyPassword;


    @Value(value = "${spring.kafka.commit-time:1500}")
    private String commitTime;


    @Bean
    public Properties streamsConfiguration() {
        final Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "deduplication-kafka");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);


        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,  new JsonSerde<>(CliperDTO.class).getClass());
        streamsConfiguration.put(StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, commitTime);
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


        streamsConfiguration.put("security.protocol", securityProtocol);
        streamsConfiguration.put("ssl.truststore.location", trustStoreLocation);
        streamsConfiguration.put("ssl.truststore.password", trustStorePassword);
        streamsConfiguration.put("ssl.keystore.location", keyStoreLocation);
        streamsConfiguration.put("ssl.keystore.password", keyStorePassword);
        streamsConfiguration.put("ssl.key.password", keyPassword);
        return streamsConfiguration;
    }

    @Bean
    public StreamsBuilder streamsBuilder() {
        return new StreamsBuilder();
    }

    @Bean
    public KafkaStreams dedupConfig(Properties streamsConfiguration, StreamsBuilder streamsBuilder) throws IOException {
        final Topology topology = streamsBuilder.build();

        KafkaStreams streams = new KafkaStreams(topology, new StreamsConfig(streamsConfiguration));
        streams.start();
        return streams;
    }

}
