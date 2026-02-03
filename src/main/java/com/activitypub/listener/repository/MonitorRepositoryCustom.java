package com.activitypub.listener.repository;

import com.activitypub.listener.model.Monitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom monitor list with optional filters and sort.
 */
public interface MonitorRepositoryCustom {

    Page<Monitor> findMonitors(
            String search,
            String monitorTypeId,
            Long productId,
            String dataSourceId,
            Monitor.ApprovalStatus isApproved,
            Boolean paused,
            Long userId,
            String sortBy,
            String orderBy,
            Pageable pageable
    );
}
