package com.orange;

import com.orange.dto.CliperDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

@Slf4j
@ComponentScan(basePackages = {"com.orange"})
@Configuration
@EnableKafkaStreams
public class CliperDedupConfiguration {

    public static final Serde<String> STRING_SERDE = Serdes.String();

    @Value(value = "${spring.kafka.bootstrap-servers:instance-1:9093}")
    private String bootstrapServers;

    @Value(value = "${spring.kafka.security.enabled:false}")
    private boolean securityEnabled;

    @Value(value = "${spring.kafka.security.protocol:SSL}")
    private String securityProtocol;

    @Value(value = "${spring.kafka.security.ssl.trust-store-location:/data/certs/truststore.jks}")
    private String trustStoreLocation;

    @Value(value = "${spring.kafka.security.ssl.trust-store-password:password}")
    private String trustStorePassword;

    @Value(value = "${spring.kafka.security.ssl.key-store-location:/data/certs/keystore.jks}")
    private String keyStoreLocation;

    @Value(value = "${spring.kafka.security.ssl.key-store-password:password}")
    private String keyStorePassword;

    @Value(value = "${spring.kafka.security.ssl.key-password:password}")
    private String keyPassword;

    @Value(value = "${spring.kafka.commit-time:1500}")
    private String commitTime;

    @Value(value = "${spring.kafka.state-store:./kafka-state}")
    private String stateStore;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfig(@Value("${spring.kafka.bootstrap-servers}") final String bootstrapServers) {
        val props = new HashMap<String, Object>();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "deduplication-kafka");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, STRING_SERDE.getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, new JsonSerde<>(CliperDTO.class).getClass());
        props.put(StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, commitTime);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try {
            var stateDirectory = Path.of(stateStore);

            // Create the directory if it doesn't exist
            if (!Files.exists(stateDirectory)) {
                stateDirectory = Files.createDirectories(stateDirectory);
            }
            props.put(StreamsConfig.STATE_DIR_CONFIG, stateDirectory.toAbsolutePath().toString());
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot create temporary directory", e);
        }

        if (securityEnabled) {
            log.info("kafkaStreamsConfig - SSL enabled");
            props.put("security.protocol", securityProtocol);
            props.put("ssl.truststore.location", trustStoreLocation);
            props.put("ssl.truststore.password", trustStorePassword);
            props.put("ssl.keystore.location", keyStoreLocation);
            props.put("ssl.keystore.password", keyStorePassword);
            props.put("ssl.key.password", keyPassword);
        }

        return new KafkaStreamsConfiguration(props);
    }

}
