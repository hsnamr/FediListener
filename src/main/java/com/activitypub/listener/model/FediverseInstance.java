package com.activitypub.listener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "fediverse_instances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FediverseInstance {
    
    @Id
    private String id;
    
    private String instanceUrl;
    
    private String instanceType;
    
    private String nodeinfoUrl;
    
    private Map<String, Object> nodeinfoData;
    
    @Builder.Default
    private Integer rateLimitPerMinute = 300;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private LocalDateTime lastHealthCheck;
    
    @Builder.Default
    private String healthStatus = "healthy";
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
