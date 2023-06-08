package com.orange.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CliperDTO {
    private String identifier;
    private long messageCreationTime;
    private String status;
    private String entityId;
    private List<ControlParameterDTO> params = new  ArrayList<>();
}
