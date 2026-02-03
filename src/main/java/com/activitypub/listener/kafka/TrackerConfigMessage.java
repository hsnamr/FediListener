package com.activitypub.listener.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Message sent to data collection engine (topic: tracker-new / tracker-update).
 * See SPECIFICATION §7.1.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerConfigMessage {

    private String trackerId;
    private String dataSourceName;
    private String monitorType;
    private String keywords;
    private String spamKeywords;
    private Boolean shouldCollect;
    private Long userId;
    private String dbHostname;
    private String dbName;
    private Map<String, Object> extra;
}
