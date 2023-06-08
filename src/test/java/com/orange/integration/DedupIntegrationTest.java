package com.orange.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
//import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.kafka.core.ConsumerFactory;
import com.orange.processor.DedupTopology;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.orange.dto.CliperDTO;
import static com.orange.util.TestEventData.buildCliperDTO;


@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { TestConfiguration.class, DedupTopology.class })
@EmbeddedKafka(controlledShutdown = true, brokerProperties = { "group.id=cliper-dedup" }, topics = { "CLIPER_TOPIC", "CLIPER_TOPIC_EVENT" })
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
    public void testKafkaStreams() throws Exception {

        // Three clipersDTO
        CliperDTO cliper1 = buildCliperDTO(UUID.randomUUID().toString(), "PENDING", "cliper-1");
        sendMessage(CLIPER_DEDUP_TEST_TOPIC, cliper1);
        CliperDTO cliper2 = buildCliperDTO(UUID.randomUUID().toString(), "PENDING", "cliper-2");
        sendMessage(CLIPER_DEDUP_TEST_TOPIC, cliper2);
        CliperDTO cliper3 = buildCliperDTO(UUID.randomUUID().toString(), "PENDING", "cliper-3");
        sendMessage(CLIPER_DEDUP_TEST_TOPIC, cliper3);
/*
        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
        .until(cliperDedupReceiver.counter::get, equalTo(1));
*/
    }


    /**
     * Send the given payment event to the given topic.
     */
    private SendResult sendMessage(String topic, CliperDTO event) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String payload = objectMapper.writeValueAsString(event);
        List<Header> headers = new ArrayList<>();
        final ProducerRecord<String, CliperDTO> record = new ProducerRecord(topic, null, event.getIdentifier(), event, headers);

        final SendResult result = (SendResult)testKafkaTemplate.send(record).get();
        final RecordMetadata metadata = result.getRecordMetadata();

        log.debug(String.format("Sent record(key=%s value=%s) meta(topic=%s, partition=%d, offset=%d)",
                                record.key(), record.value(), metadata.topic(), metadata.partition(), metadata.offset()));

        return result;
    }
}
