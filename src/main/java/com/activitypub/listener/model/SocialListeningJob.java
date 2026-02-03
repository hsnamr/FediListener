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
import java.util.List;
import java.util.Map;

/**
 * Async job for public social listening API. §4.4.3
 */
@Document(collection = "social_listening_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialListeningJob {

    @Id
    private String id;

    private String monitorId;
    private Long userId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private List<String> widgetsNames;
    private Map<String, Object> widgetData;
    private String nextJobId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
