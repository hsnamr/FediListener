package com.activitypub.listener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "metrics_v2")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metric {
    
    @Id
    private String id;
    
    private String name;
    
    private String displayName;
    
    private String description;
    
    @DBRef
    private MonitorType monitorType;
    
    @DBRef
    private DataSource dataSource;
    
    private Long dashboardPagesId;
    
    private String chartType;
    
    @Builder.Default
    private Boolean active = true;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
