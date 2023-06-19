package com.orange.properties;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties("dedup")
@Getter
@Setter
@Validated
public class CliperDedupProperties {
    @NotNull private String id;
    @NotNull private String dedupInboundTopic;
    @NotNull private String dedupOutboundTopic;
}
