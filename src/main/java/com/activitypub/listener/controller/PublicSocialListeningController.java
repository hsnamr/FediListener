package com.activitypub.listener.controller;

import com.activitypub.listener.dto.ApiResponse;
import com.activitypub.listener.dto.SocialListeningRequestDTO;
import com.activitypub.listener.model.SocialListeningJob;
import com.activitypub.listener.service.SocialListeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public API for async social listening data. §4.4.3
 */
@RestController
@RequestMapping("/public-api/social-listening")
@RequiredArgsConstructor
public class PublicSocialListeningController {

    private final SocialListeningService socialListeningService;

    @PostMapping("/data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createJob(
            @Valid @RequestBody SocialListeningRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        SocialListeningJob job = socialListeningService.createJob(request, userId);
        Map<String, Object> data = Map.of(
                "job_id", job.getId(),
                "monitor_id", job.getMonitorId(),
                "widgets_names", job.getWidgetsNames() != null ? job.getWidgetsNames() : java.util.List.of()
        );
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .data(data)
                .build());
    }

    @GetMapping("/data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobStatus(
            @RequestParam String jobId,
            @RequestParam String monitorId) {

        Map<String, Object> data = socialListeningService.getJobStatus(jobId, monitorId);
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .data(data)
                .build());
    }
}
