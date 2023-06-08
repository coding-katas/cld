package com.orange.processor;


import com.orange.dto.CliperDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.WindowStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.kafka.support.serializer.JsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import java.time.Duration;
import org.apache.kafka.common.utils.Bytes;
import org.springframework.beans.factory.annotation.Value;

@Component
@Slf4j
@RequiredArgsConstructor
public class DedupTopology {


    @Value(value = "${dedup.windowTime:1200}")
    private Long windowTime;

    @Value(value = "${dedup.windowTime:200}")
    private Long graceTime;

    @Value(value = "${dedup.dedupInboundTopic:CLIPER_TOPIC}")
    private String dedupInboundTopic;

    @Value(value = "${dedup.dedupOutboundTopic:CLIPER_TOPIC_EVENT}")
    private String dedupOutboundTopic;

    private static final String STORE_NAME = "eventId-store";

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {

        JsonSerde cliperSerdes = new JsonSerde<>(CliperDTO.class);

        KStream<String, CliperDTO> source = streamsBuilder.stream(dedupInboundTopic, Consumed.with(Serdes.String(), cliperSerdes));

        KTable reducedTable =
            source
            .selectKey((key, value) -> value.getEntityId())
            .groupByKey(Grouped.with(Serdes.String(), cliperSerdes))
            .windowedBy(TimeWindows.of(Duration.ofMillis(windowTime)).grace(Duration.ofMillis(graceTime)))
            .reduce((v1, v2) -> v2, Materialized.<String, CliperDTO, WindowStore<Bytes, byte[]>>as(STORE_NAME)
                    .withValueSerde(cliperSerdes)
                    .withKeySerde(Serdes.String()));
        KStream<String, CliperDTO> outputStream = reducedTable.toStream().map((windowedId, value) -> new KeyValue<>(windowedId.toString(), value));
        outputStream.to(dedupOutboundTopic, Produced.with(Serdes.String(), cliperSerdes));

    }
}

