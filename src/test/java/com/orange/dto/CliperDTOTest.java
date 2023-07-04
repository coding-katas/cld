package com.orange.dto;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliperDTOTest {

    @Test
    void testToString() {
        val sut = CliperDTO.builder()
                .identifier("id")
                .messageCreationTime(1)
                .status("status")
                .entityId("entityId")
                .build();

        assertEquals("CliperDTO(identifier=id, messageCreationTime=1, status=status, entityId=entityId, params=[])",
                sut.toString());
    }
}