package com.activitypub.listener.service;

import com.activitypub.listener.dto.CreateSavedFilterDTO;
import com.activitypub.listener.dto.SavedFilterDTO;
import com.activitypub.listener.exception.ResourceNotFoundException;
import com.activitypub.listener.model.Monitor;
import com.activitypub.listener.model.SavedFilter;
import com.activitypub.listener.repository.MonitorRepository;
import com.activitypub.listener.repository.SavedFilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Saved filters per monitor. §4.5.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {

    @Value("${social.listening.max-filters-per-monitor:20}")
    private int maxFiltersPerMonitor;

    private final SavedFilterRepository savedFilterRepository;
    private final MonitorRepository monitorRepository;

    public List<SavedFilterDTO> getFilters(String monitorId, Long userId) {
        ensureMonitorExistsAndOwnedByUser(monitorId, userId);
        return savedFilterRepository.findByMonitorIdOrderByCreatedAtDesc(monitorId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public SavedFilterDTO saveFilter(String monitorId, Long userId, CreateSavedFilterDTO dto) {
        ensureMonitorExistsAndOwnedByUser(monitorId, userId);
        long count = savedFilterRepository.countByMonitorId(monitorId);
        if (count >= maxFiltersPerMonitor) {
            throw new IllegalStateException("Filter limit exceeded for this monitor (max: " + maxFiltersPerMonitor + ")");
        }
        SavedFilter filter = SavedFilter.builder()
                .monitorId(monitorId)
                .name(dto.getName())
                .filters(dto.getFilters())
                .userId(userId)
                .build();
        filter = savedFilterRepository.save(filter);
        log.info("Saved filter {} for monitor {}", filter.getId(), monitorId);
        return toDTO(filter);
    }

    private void ensureMonitorExistsAndOwnedByUser(String monitorId, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + monitorId));
        if (userId != null && !monitor.getUserId().equals(userId)) {
            throw new IllegalStateException("Monitor does not belong to user");
        }
    }

    private SavedFilterDTO toDTO(SavedFilter f) {
        return SavedFilterDTO.builder()
                .id(f.getId())
                .monitorId(f.getMonitorId())
                .name(f.getName())
                .filters(f.getFilters())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
