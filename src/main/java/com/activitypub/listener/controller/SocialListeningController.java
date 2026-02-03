package com.activitypub.listener.controller;

import com.activitypub.listener.dto.ApiResponse;
import com.activitypub.listener.dto.SocialListeningRequestDTO;
import com.activitypub.listener.dto.SocialListeningResponseDTO;
import com.activitypub.listener.dto.WidgetDTO;
import com.activitypub.listener.service.SocialListeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/social-listening")
@RequiredArgsConstructor
public class SocialListeningController {

    private final SocialListeningService socialListeningService;

    /**
     * Request social listening data. Sends request to analytics engine via Kafka,
     * returns topic and consumer group for frontend connection. §4.4.1
     */
    @PostMapping("/data")
    public ResponseEntity<ApiResponse<SocialListeningResponseDTO>> requestData(
            @Valid @RequestBody SocialListeningRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        SocialListeningResponseDTO data = socialListeningService.requestData(request, userId);
        return ResponseEntity.ok(ApiResponse.<SocialListeningResponseDTO>builder()
                .data(data)
                .message("Request accepted")
                .build());
    }

    /**
     * Get available widgets/metrics for a monitor and data source. §4.4.2
     */
    @GetMapping("/widgets")
    public ResponseEntity<ApiResponse<Map<String, List<WidgetDTO>>>> getWidgets(
            @RequestParam String monitorId,
            @RequestParam String dataSource,
            @RequestParam String pageName) {

        List<WidgetDTO> widgets = socialListeningService.getWidgets(monitorId, dataSource, pageName);
        return ResponseEntity.ok(ApiResponse.<Map<String, List<WidgetDTO>>>builder()
                .data(Map.of("widgets", widgets))
                .build());
    }
}
