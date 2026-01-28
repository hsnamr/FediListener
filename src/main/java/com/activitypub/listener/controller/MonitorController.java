package com.activitypub.listener.controller;

import com.activitypub.listener.dto.*;
import com.activitypub.listener.service.MonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitors")
@RequiredArgsConstructor
@Slf4j
public class MonitorController {
    
    private final MonitorService monitorService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<MonitorDTO>> createMonitor(
            @Valid @RequestBody CreateMonitorDTO dto,
            @RequestHeader("X-User-Id") Long userId) {
        
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
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String orderBy) {
        
        Sort sort = Sort.by(sortBy != null ? sortBy : "createdAt");
        if ("desc".equalsIgnoreCase(orderBy)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        
        Pageable pageable = PageRequest.of(page - 1, Math.min(perPage, 100), sort);
        PaginationResponse<MonitorDTO> monitors = monitorService.listMonitors(pageable, search);
        
        ApiResponse<PaginationResponse<MonitorDTO>> response = ApiResponse.<PaginationResponse<MonitorDTO>>builder()
                .data(monitors)
                .build();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MonitorDTO>> getMonitor(
            @PathVariable String id) {
        
        MonitorDTO monitor = monitorService.getMonitor(id);
        ApiResponse<MonitorDTO> response = ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .build();
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MonitorDTO>> updateMonitor(
            @PathVariable String id,
            @Valid @RequestBody UpdateMonitorDTO dto) {
        
        MonitorDTO monitor = monitorService.updateMonitor(id, dto);
        ApiResponse<MonitorDTO> response = ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .message("Monitor updated successfully")
                .build();
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(
            @PathVariable String id) {
        
        monitorService.deleteMonitor(id);
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
            @PathVariable String id) {
        
        MonitorDTO monitor = monitorService.resumeMonitor(id);
        ApiResponse<MonitorDTO> response = ApiResponse.<MonitorDTO>builder()
                .data(monitor)
                .message("Monitor resumed successfully")
                .build();
        return ResponseEntity.ok(response);
    }
}
