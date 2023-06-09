package com.orange.processor;

import com.orange.dto.CliperDTO;
import com.orange.properties.CliperDedupProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.orange.CliperDedupConfiguration.STRING_SERDE;


@Component
@Slf4j
@RequiredArgsConstructor
public class DedupTopology {

    @Autowired
    private final CliperDedupProperties properties;

    private static final String STORE_NAME = "eventId-store";

    @Value(value = "${dedup.windowTime:1200}")
    private Long windowTime;

    @Value(value = "${dedup.graceTime:200}")
    private Long graceTime;

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        val cliperSerdes = new JsonSerde<>(CliperDTO.class);

        KStream<String, CliperDTO> messageStream = streamsBuilder
                .stream(properties.getDedupInboundTopic(), Consumed.with(STRING_SERDE, cliperSerdes))
                .peek((key, message) -> log.debug("Event received with key={}, message={}", key, message));

        messageStream
                .selectKey((key, value) -> value.getEntityId())
                .groupByKey(Grouped.with(STRING_SERDE, cliperSerdes))
                .windowedBy(TimeWindows.of(Duration.ofMillis(windowTime)).grace(Duration.ofMillis(graceTime)))
                .reduce((v1, v2) -> v2, Materialized.<String, CliperDTO, WindowStore<Bytes, byte[]>>as(STORE_NAME)
                        .withValueSerde(cliperSerdes)
                        .withKeySerde(STRING_SERDE))
                .toStream().map((windowedId, value) -> new KeyValue<>(windowedId.toString(), value))
                .peek((key, message) -> log.debug("Event After reduce with key={}, value={}", key, message))
                .to(properties.getDedupOutboundTopic(), Produced.with(STRING_SERDE, cliperSerdes));

    }
}
