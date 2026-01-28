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

@Document(collection = "data_sources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSource {
    
    @Id
    private String id;
    
    private String source;
    
    private String category;
    
    private String description;
    
    @Builder.Default
    private Boolean active = true;
    
    private String instanceUrl;
    
    private String instanceType;
    
    private String apiVersion;
    
    @Builder.Default
    private Integer rateLimitPerMinute = 300;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
