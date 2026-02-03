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
public class SavedFilterDTO {
    private String id;
    private String monitorId;
    private String name;
    private Map<String, Object> filters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
