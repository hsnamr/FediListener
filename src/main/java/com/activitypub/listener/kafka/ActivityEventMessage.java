package com.activitypub.listener.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Collected activity event sent to activities topic.
 * See SPECIFICATION §7.1.1 and IMPLEMENTATION_PLAN §2.2.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEventMessage {

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
}
