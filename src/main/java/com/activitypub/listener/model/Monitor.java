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
import java.util.ArrayList;
import java.util.List;

@Document(collection = "monitors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Monitor {
    
    @Id
    private String id;
    
    private String name;
    
    @DBRef
    private MonitorType monitorType;
    
    private Long userId;
    
    private Long creatorId;
    
    private Long productId;
    
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Builder.Default
    private Boolean paused = false;
    
    @Builder.Default
    private ApprovalStatus isApproved = ApprovalStatus.UNAPPROVED;
    
    @Builder.Default
    private MonitorFeatureType monitorFeatureType = MonitorFeatureType.NORMAL;
    
    private String languages;
    
    @Builder.Default
    private Boolean exactSearch = false;
    
    @Builder.Default
    private Boolean autoSummaryWidget = false;
    
    @Builder.Default
    private Integer version = 1;
    
    @Builder.Default
    private List<Keyword> keywords = new ArrayList<>();
    
    @Builder.Default
    private List<AccountAnalysis> accountAnalyses = new ArrayList<>();
    
    @Builder.Default
    private List<Regional> regionals = new ArrayList<>();

    @Builder.Default
    private List<ManagedAccount> managedAccounts = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum ApprovalStatus {
        UNAPPROVED,
        APPROVED,
        APPROVED_REJECTED,
        UNAPPROVED_REJECTED
    }
    
    public enum MonitorFeatureType {
        NORMAL,
        SINGLE_TWEET,
        EXPLORE
    }
}
