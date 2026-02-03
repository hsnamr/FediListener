package com.activitypub.listener.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event sent when monitor approval status changes (§7.3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorLifecycleMessage {
    private String monitorId;
    private String action; // APPROVED, REJECTED
    private String previousStatus;
    private String newStatus;
    private Long userId;
    private LocalDateTime at;
}
