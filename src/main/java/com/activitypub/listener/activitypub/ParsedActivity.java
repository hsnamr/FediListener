package com.activitypub.listener.activitypub;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Parsed ActivityStreams activity (Create, Update, Delete, Announce, Like, etc.)
 * with extracted object and content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedActivity {

    private String activityId;
    private String activityType;
    private String actorId;
    private String objectId;
    private String objectType;
    private String content;
    private LocalDateTime publishedAt;
    private String instanceUrl;
    private Map<String, Object> rawData;
}
