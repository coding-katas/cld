package com.orange.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.orange.CliperDedupConfiguration;
import com.orange.dto.CliperDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.orange.util.TestEventData.buildCliperDTO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { CliperDedupConfiguration.class })
@EmbeddedKafka(controlledShutdown = true, topics = { "CLIPER_TOPIC", "CLIPER_TOPIC_EVENT" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class DedupIntegrationTest {

    private final static String CLIPER_DEDUP_TEST_TOPIC = "CLIPER_TOPIC";

    @Autowired
    private KafkaTemplate testKafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaCliperDedupListener cliperDedupReceiver;

    @Configuration
    static class TestConfig {

        @Bean
        public KafkaCliperDedupListener cliperDedupReceiver() {
            return new KafkaCliperDedupListener();
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(final ConsumerFactory<String, String> consumerFactory) {
            final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory();
            factory.setConsumerFactory(consumerFactory);
            return factory;
        }

        @Bean
        public KafkaTemplate<String, String> kafkaTemplate(final ProducerFactory<String, String> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        public ConsumerFactory<String, String> consumerFactory(@Value("${spring.kafka.bootstrap-servers}") final String bootstrapServers) {
            final Map<String, Object> config = new HashMap<>();
            config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(ConsumerConfig.GROUP_ID_CONFIG, "dedup-kafka");
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ProducerFactory<String, String> producerFactory(@Value("${spring.kafka.bootstrap-servers}") final String bootstrapServers) {
            final Map<String, Object> config = new HashMap<>();
            config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new DefaultKafkaProducerFactory<>(config);
        }
    }

    public static class KafkaCliperDedupListener {
        AtomicInteger counter = new AtomicInteger(0);

        @KafkaListener(groupId = "cliper-dedup", topics = "CLIPER_TOPIC_EVENT", autoStartup = "true")
        void receive(@Payload final String payload, @Headers final MessageHeaders headers) {
            log.info("KafkaCliperDedupListener - Received message: " + payload);
            counter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() {
        // Wait until the partitions are assigned.
        registry.getListenerContainers()
        .stream()
        .forEach(container -> ContainerTestUtils
                 .waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));

        cliperDedupReceiver.counter.set(0);
    }

    /**
     * Send a number of messages to the inbound messagess topic.
     */
    @Test
    public void testKafkaStreams()  throws Exception {
        String testId = UUID.randomUUID().toString();

        CliperDTO cliper1 = buildCliperDTO(testId, "PENDING", "cliper-1");
        sendMessage(CLIPER_DEDUP_TEST_TOPIC, cliper1);
        CliperDTO cliper2 = buildCliperDTO(testId, "PENDING", "cliper-1");
        sendMessage(CLIPER_DEDUP_TEST_TOPIC, cliper2);
        CliperDTO cliper3 = buildCliperDTO(UUID.randomUUID().toString(), "PENDING", "cliper-2");
        sendMessage(CLIPER_DEDUP_TEST_TOPIC, cliper3);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
        .until(cliperDedupReceiver.counter::get, equalTo(2));
        assertThat(cliperDedupReceiver.counter.get(), equalTo(2));
    }

    /**
     * Send the given event to the given topic.
     */
    private SendResult sendMessage(String topic, CliperDTO event) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String payload = objectMapper.writeValueAsString(event);
        List<Header> headers = new ArrayList<>();
        final ProducerRecord<String, CliperDTO> record = new ProducerRecord(topic, null, event.getIdentifier(), payload, headers);

        final SendResult result = (SendResult)testKafkaTemplate.send(record).get();
        final RecordMetadata metadata = result.getRecordMetadata();

        log.info(String.format("Sent record(key=%s value=%s) meta(topic=%s, partition=%d, offset=%d)",
                               record.key(), record.value(), metadata.topic(), metadata.partition(), metadata.offset()));

        return result;
    }
}
