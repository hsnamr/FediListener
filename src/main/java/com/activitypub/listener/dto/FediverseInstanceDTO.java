package com.activitypub.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FediverseInstanceDTO {
    private String id;
    private String instanceUrl;
    private String instanceType;
    private String nodeinfoUrl;
    private Integer rateLimitPerMinute;
    private Boolean isActive;
    private String healthStatus;
    private java.time.LocalDateTime lastHealthCheck;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
