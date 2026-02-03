package com.activitypub.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response with topic and consumer group for frontend connection. §3.9, §4.4.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialListeningResponseDTO {
    private String topic;
    private String consumerGroup;
    private String monitorId;
    private Boolean manualTopicsEnabled;
    private Integer monitorTopicsUsed;
}
