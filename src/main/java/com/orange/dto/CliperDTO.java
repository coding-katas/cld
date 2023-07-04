package com.orange.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CliperDTO {
    private String identifier;
    private long messageCreationTime;
    private String status;
    private String entityId;
    @Builder.Default
    private List<ControlParameterDTO> params = new ArrayList<>();
}
