package com.activitypub.listener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Persisted ActivityPub activity collected from Fediverse instances.
 * See SPECIFICATION §7.1.1 and IMPLEMENTATION_PLAN §8.1.
 */
@Document(collection = "collected_activities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectedActivity {

    @Id
    private String id;

    /** Full ActivityPub activity ID (e.g. https://instance/users/x/statuses/123) */
    @Indexed(unique = true)
    private String activityId;

    /** Activity type: Create, Update, Delete, Announce, Like, Follow, Undo, etc. */
    private String activityType;

    /** Actor ID who performed the activity */
    @Indexed
    private String actorId;

    /** Object ID (e.g. note, article) if applicable */
    private String objectId;

    /** Object type: Note, Article, Video, Comment, etc. */
    private String objectType;

    /** Extracted content text (e.g. note content) */
    private String content;

    /** Published timestamp from activity/object */
    @Indexed
    private LocalDateTime publishedAt;

    /** Instance base URL (e.g. https://mastodon.social) */
    @Indexed
    private String instanceUrl;

    /** Monitor ID that triggered collection, if applicable */
    @Indexed
    private String monitorId;

    /** Full activity JSON for replay or analytics */
    private Map<String, Object> rawData;

    @CreatedDate
    private LocalDateTime createdAt;
}
