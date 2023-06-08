package com.orange.util;

import com.orange.dto.CliperDTO;
import com.orange.dto.ControlParameterDTO;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestEventData {

    public static CliperDTO buildCliperDTO(String identifier, String status, String entityId) {
        // Create the control parameter
        ControlParameterDTO controlParameter = ControlParameterDTO.builder()
                                               .key("application_name")
                                               .value("tcrm")
                                               .build();

        // Create the params list and add the control parameter
        List<ControlParameterDTO> params = new ArrayList<>();
        params.add(controlParameter);

        // Build the CliperDTO object
        CliperDTO cliperDTO = CliperDTO.builder()
                              .identifier(identifier)
                              .status(status)
                              .entityId(entityId)
                              .messageCreationTime(System.currentTimeMillis())
                              .params(params)
                              .build();

        return cliperDTO;
    }
}
