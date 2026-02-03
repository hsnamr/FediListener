package com.activitypub.listener.controller;

import com.activitypub.listener.dto.*;
import com.activitypub.listener.service.MonitorService;
import com.activitypub.listener.service.SavedFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/monitors")
@RequiredArgsConstructor
@Slf4j
public class MonitorController {

    private final MonitorService monitorService;
    private final SavedFilterService savedFilterService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<MonitorDTO>> createMonitor(
            @Valid @RequestBody CreateMonitorDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        MonitorDTO monitor = monitorService.createMonitor(dto, userId);
        ApiResponse<MonitorDTO> response = ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .message("Monitor created successfully")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<MonitorDTO>>> listMonitors(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String monitorTypeId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String dataSourceId,
            @RequestParam(required = false) String isApproved,
            @RequestParam(required = false) Boolean paused,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String orderBy,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {

        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        Pageable pageable = PageRequest.of(page - 1, Math.min(perPage, 100));
        com.activitypub.listener.model.Monitor.ApprovalStatus approvalStatus = null;
        if (isApproved != null && !isApproved.isEmpty()) {
            try {
                approvalStatus = com.activitypub.listener.model.Monitor.ApprovalStatus.valueOf(isApproved.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // leave null if invalid
            }
        }
        PaginationResponse<MonitorDTO> monitors = monitorService.listMonitors(
                pageable, search, monitorTypeId, productId, dataSourceId, approvalStatus, paused, userId, sortBy, orderBy);

        ApiResponse<PaginationResponse<MonitorDTO>> response = ApiResponse.<PaginationResponse<MonitorDTO>>builder()
                .data(monitors)
                .build();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MonitorDTO>> getMonitor(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        MonitorDTO monitor = monitorService.getMonitor(id, userId);
        ApiResponse<MonitorDTO> response = ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<ApiResponse<PaginationResponse<CollectedActivityDTO>>> getMonitorActivities(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        PaginationResponse<CollectedActivityDTO> data = monitorService.getActivitiesForMonitor(id, page, perPage, userId);
        return ResponseEntity.ok(ApiResponse.<PaginationResponse<CollectedActivityDTO>>builder().data(data).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MonitorDTO>> updateMonitor(
            @PathVariable String id,
            @Valid @RequestBody UpdateMonitorDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        MonitorDTO monitor = monitorService.updateMonitor(id, dto, userId);
        ApiResponse<MonitorDTO> response = ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .message("Monitor updated successfully")
                .build();
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        monitorService.deleteMonitor(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<MonitorDTO>> pauseMonitor(
            @PathVariable String id) {
        
        MonitorDTO monitor = monitorService.pauseMonitor(id);
        ApiResponse<MonitorDTO> response = ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .message("Monitor paused successfully")
                .build();
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<MonitorDTO>> resumeMonitor(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        MonitorDTO monitor = monitorService.resumeMonitor(id, userId);
        ApiResponse<MonitorDTO> response = ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .message("Monitor resumed successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<MonitorDTO>> approveMonitor(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        MonitorDTO monitor = monitorService.approveMonitor(id, userId);
        return ResponseEntity.ok(ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .message("Monitor approved successfully")
                .build());
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<MonitorDTO>> rejectMonitor(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        MonitorDTO monitor = monitorService.rejectMonitor(id, userId);
        return ResponseEntity.ok(ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .message("Monitor rejected successfully")
                .build());
    }

    @GetMapping("/{id}/filters")
    public ResponseEntity<ApiResponse<List<SavedFilterDTO>>> getFilters(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        List<SavedFilterDTO> filters = savedFilterService.getFilters(id, userId);
        return ResponseEntity.ok(ApiResponse.<List<SavedFilterDTO>>builder().data(filters).build());
    }

    @PostMapping("/{id}/filters")
    public ResponseEntity<ApiResponse<SavedFilterDTO>> saveFilter(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody CreateSavedFilterDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : headerUserId;
        SavedFilterDTO saved = savedFilterService.saveFilter(id, userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<SavedFilterDTO>builder()
                        .data(saved)
                        .message("Filter saved successfully")
                        .build());
    }
}
