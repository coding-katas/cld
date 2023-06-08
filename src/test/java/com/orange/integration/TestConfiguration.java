package com.orange.integration;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.STATE_DIR_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import com.orange.dto.CliperDTO;
import java.util.Properties;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import java.util.concurrent.atomic.AtomicInteger;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

//@ComponentScan(basePackages = {"com.orange"})
//@EnableKafkaStreams

@Slf4j
@ComponentScan(basePackageClasses = DedupIntegrationTest.class)
@Configuration
public class TestConfiguration {
    private Path stateDirectory;
/*
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfig(@Value("${kafka.bootstrap-servers}") final String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(APPLICATION_ID_CONFIG, "cliper-dedup");
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, "30000");        
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        try {
            this.stateDirectory = Files.createTempDirectory("kafka-streams");
            props.put(STATE_DIR_CONFIG, this.stateDirectory.toAbsolutePath()
                .toString());
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot create temporary directory", e);
        } 
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG,  new JsonSerde<>(CliperDTO.class).getClass());
        return new KafkaStreamsConfiguration(props);
    }
*/
    @Bean
    public Properties streamsConfiguration(@Value("${kafka.bootstrap-servers}") final String bootstrapServers) {
        final Properties props = new Properties();
        props.put(APPLICATION_ID_CONFIG, "cliper-dedup");
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, "30000");
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        try {
            this.stateDirectory = Files.createTempDirectory("kafka-streams");
            props.put(STATE_DIR_CONFIG, this.stateDirectory.toAbsolutePath()
                .toString());
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot create temporary directory", e);
        }
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG,  new JsonSerde<>(CliperDTO.class).getClass());
        return props;
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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CliperDTO> kafkaListenerContainerFactory(final ConsumerFactory<String, CliperDTO> consumerFactory) {
        final ConcurrentKafkaListenerContainerFactory<String, CliperDTO> factory = new ConcurrentKafkaListenerContainerFactory();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, CliperDTO> kafkaTemplate(final ProducerFactory<String, CliperDTO> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public  ConsumerFactory<String, CliperDTO> consumerFactory(@Value("${kafka.bootstrap-servers}") final String bootstrapServers) {
        final Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "cliper-dedup");
	config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ProducerFactory<String, CliperDTO> producerFactory(@Value("${kafka.bootstrap-servers}") final String bootstrapServers) {
        final Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry() {
        return new KafkaListenerEndpointRegistry();
    }

}
