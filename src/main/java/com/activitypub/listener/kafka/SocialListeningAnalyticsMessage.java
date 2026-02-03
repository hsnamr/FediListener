package com.activitypub.listener.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Message sent to analytics engine (topic: social-listening). §7.1.2, §6.4.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialListeningAnalyticsMessage {
    private Long startDate;
    private Long endDate;
    private String monitorId;
    private String dataSourceId;
    private String dataSourceName;
    private Long userId;
    private String dbHostname;
    private String dbName;
    private String routingKey;
    private String topic;
    private String consumerGroup;
    private String filters; // JSON string
    private String pageName;
    private String monitorType;
    private List<Long> manualTopics;
    private String productName;
    private List<String> widgetsNames;
    private Integer pageNumber;
}
