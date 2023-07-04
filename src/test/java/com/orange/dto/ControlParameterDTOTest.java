package com.orange.dto;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlParameterDTOTest {

    @Test
    void testToString() {
        val sut = ControlParameterDTO.builder()
                .key("key")
                .value("value")
                .build();

        assertEquals("ControlParameterDTO(key=key, value=value)", sut.toString());
    }
}