package com.activitypub.listener.controller;

import com.activitypub.listener.activitypub.ActivityPubService;
import com.activitypub.listener.dto.ApiResponse;
import com.activitypub.listener.model.ActivityPubActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activitypub")
@RequiredArgsConstructor
@Slf4j
public class ActivityPubController {
    
    private final ActivityPubService activityPubService;
    
    @PostMapping("/actors/discover")
    public ResponseEntity<ApiResponse<ActivityPubActor>> discoverActor(
            @RequestParam String resource) {
        
        ActivityPubActor actor = activityPubService.discoverAndSaveActor(resource);
        ApiResponse<ActivityPubActor> response = ApiResponse.<ActivityPubActor>builder()
                .data(actor)
                .message("Actor discovered successfully")
                .build();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/actors/{actorId}")
    public ResponseEntity<ApiResponse<ActivityPubActor>> getActor(
            @PathVariable String actorId) {
        
        ActivityPubActor actor = activityPubService.getActor(actorId);
        ApiResponse<ActivityPubActor> response = ApiResponse.<ActivityPubActor>builder()
                .data(actor)
                .build();
        return ResponseEntity.ok(response);
    }
}
