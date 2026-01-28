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

@Document(collection = "activitypub_actors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPubActor {
    
    @Id
    private String id;
    
    private String actorId;
    
    private String username;
    
    private String instanceUrl;
    
    private String actorType;
    
    private String inboxUrl;
    
    private String outboxUrl;
    
    private String sharedInboxUrl;
    
    private Map<String, Object> profileData;
    
    private LocalDateTime lastCheckedAt;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
