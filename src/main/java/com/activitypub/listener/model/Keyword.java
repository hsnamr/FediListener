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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "keywords")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Keyword {
    
    @Id
    private String id;
    
    private String monitorId;
    
    @DBRef
    private DataSource dataSource;
    
    private String keywords;
    
    private String spamKeywords;
    
    @Builder.Default
    private Boolean shouldCollect = true;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private LocalDate oldestDate;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
