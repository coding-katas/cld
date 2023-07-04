package com.orange.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties("dedup")
@Data
@Validated
public class CliperDedupProperties {
    @NotNull
    private String id;
    @NotNull
    private String dedupInboundTopic;
    @NotNull
    private String dedupOutboundTopic;
}
