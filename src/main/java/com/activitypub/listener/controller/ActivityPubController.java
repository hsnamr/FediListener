package com.activitypub.listener.controller;

import com.activitypub.listener.activitypub.ActivityPubService;
import com.activitypub.listener.activitypub.OutboxPollingService;
import com.activitypub.listener.dto.ApiResponse;
import com.activitypub.listener.dto.PaginationResponse;
import com.activitypub.listener.dto.CollectedActivityDTO;
import com.activitypub.listener.model.ActivityPubActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/actors")
@RequiredArgsConstructor
@Slf4j
public class ActivityPubController {

    private final ActivityPubService activityPubService;
    private final OutboxPollingService outboxPollingService;

    /**
     * Discover actor by resource (e.g. acct:user@mastodon.social).
     * Aligns with README/spec: GET /api/actors/discover?resource=...
     */
    @GetMapping("/discover")
    public ResponseEntity<ApiResponse<ActivityPubActor>> discoverActorGet(
            @RequestParam String resource) {
        ActivityPubActor actor = activityPubService.discoverAndSaveActor(resource);
        ApiResponse<ActivityPubActor> response = ApiResponse.<ActivityPubActor>builder()
                .data(actor)
                .message("Actor discovered successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    /** POST discover kept for backward compatibility. */
    @PostMapping("/discover")
    public ResponseEntity<ApiResponse<ActivityPubActor>> discoverActorPost(
            @RequestParam String resource) {
        return discoverActorGet(resource);
    }

    @GetMapping("/{actorId}")
    public ResponseEntity<ApiResponse<ActivityPubActor>> getActor(
            @PathVariable String actorId) {
        ActivityPubActor actor = activityPubService.getActor(actorId);
        ApiResponse<ActivityPubActor> response = ApiResponse.<ActivityPubActor>builder()
                .data(actor)
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Get paginated activities for an actor (from collected_activities).
     * Aligns with README: GET /api/actors/{actor_id}/activities?page=1&per_page=20
     */
    @GetMapping("/{actorId}/activities")
    public ResponseEntity<ApiResponse<PaginationResponse<CollectedActivityDTO>>> getActorActivities(
            @PathVariable String actorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        PaginationResponse<CollectedActivityDTO> data = activityPubService.getActorActivities(actorId, page, perPage);
        return ResponseEntity.ok(ApiResponse.<PaginationResponse<CollectedActivityDTO>>builder().data(data).build());
    }

    /**
     * Trigger outbox polling for an actor. New activities are persisted and sent to Kafka.
     */
    @PostMapping("/{actorId}/poll")
    public ResponseEntity<ApiResponse<Integer>> pollActorOutbox(
            @PathVariable String actorId,
            @RequestParam(required = false) String monitorId) {
        int collected = outboxPollingService.pollActorOutbox(actorId, monitorId);
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .data(collected)
                .message("Poll completed: " + collected + " new activities")
                .build());
    }
}
