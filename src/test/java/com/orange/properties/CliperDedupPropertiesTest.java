package com.orange.properties;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliperDedupPropertiesTest {

    @Test
    void cliperDedupPropertiesTest() {

        val sut = new CliperDedupProperties();
        sut.setId("id");
        sut.setDedupInboundTopic("in");
        sut.setDedupOutboundTopic("out");

        assertEquals("id", sut.getId());
        assertEquals("in", sut.getDedupInboundTopic());
        assertEquals("out", sut.getDedupOutboundTopic());
        assertEquals("CliperDedupProperties(id=id, dedupInboundTopic=in, dedupOutboundTopic=out)", sut.toString());
    }
}
