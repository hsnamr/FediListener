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

@Document(collection = "regionals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Regional {
    
    @Id
    private String id;
    
    private String monitorId;
    
    @DBRef
    private DataSource dataSource;
    
    private String mbr;
    
    @Builder.Default
    private Boolean shouldCollect = true;
    
    private LocalDate oldestDate;
    
    @Builder.Default
    private Integer totalCount = 0;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
