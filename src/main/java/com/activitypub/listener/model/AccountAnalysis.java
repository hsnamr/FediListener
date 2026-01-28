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

@Document(collection = "account_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountAnalysis {
    
    @Id
    private String id;
    
    private String monitorId;
    
    @DBRef
    private DataSource dataSource;
    
    private Long accountInfoId;
    
    private String follow;
    
    private String spamKeywords;
    
    private String excludedAccounts;
    
    @Builder.Default
    private Boolean shouldCollect = true;
    
    private LocalDate oldestDate;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
