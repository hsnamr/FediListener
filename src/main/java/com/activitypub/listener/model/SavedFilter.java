package com.activitypub.listener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Saved filter configuration for a monitor. §4.5.
 */
@Document(collection = "saved_filters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedFilter {

    @Id
    private String id;

    @Indexed
    private String monitorId;

    private String name;

    private Map<String, Object> filters;

    private Long userId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
