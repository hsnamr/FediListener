package com.activitypub.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectedActivityDTO {
    private String id;
    private String activityId;
    private String activityType;
    private String actorId;
    private String objectId;
    private String objectType;
    private String content;
    private LocalDateTime publishedAt;
    private String instanceUrl;
    private String monitorId;
    private Map<String, Object> rawData;
    private LocalDateTime createdAt;
}
