package com.activitypub.listener.activitypub;

import com.activitypub.listener.dto.CollectedActivityDTO;
import com.activitypub.listener.dto.PaginationResponse;
import com.activitypub.listener.model.ActivityPubActor;
import com.activitypub.listener.model.CollectedActivity;
import com.activitypub.listener.repository.ActivityPubActorRepository;
import com.activitypub.listener.repository.CollectedActivityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityPubService {
    
    private final ActivityPubClient activityPubClient;
    private final ActivityPubActorRepository actorRepository;
    private final CollectedActivityRepository collectedActivityRepository;
    
    /**
     * Discover and retrieve actor information
     */
    public ActivityPubActor discoverAndSaveActor(String resource) {
        log.info("Discovering actor: {}", resource);
        
        // Parse resource (acct:user@instance.com)
        String[] parts = resource.replace("acct:", "").split("@");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid resource format: " + resource);
        }
        
        String username = parts[0];
        String instanceUrl = "https://" + parts[1];
        
        // Check if actor already exists
        ActivityPubActor existingActor = actorRepository
                .findByUsernameAndInstanceUrl(username, instanceUrl)
                .orElse(null);
        
        if (existingActor != null && existingActor.getLastCheckedAt() != null) {
            // Return cached actor if recently checked (within last hour)
            if (existingActor.getLastCheckedAt().isAfter(LocalDateTime.now().minusHours(1))) {
                log.debug("Returning cached actor: {}", existingActor.getActorId());
                return existingActor;
            }
        }
        
        // Discover via WebFinger
        ActivityPubClient.WebFingerResponse webfinger = activityPubClient
                .discoverActor(resource)
                .block();
        
        if (webfinger == null || webfinger.getActorUrl() == null) {
            throw new RuntimeException("Failed to discover actor: " + resource);
        }
        
        // Get actor profile
        JsonNode actorProfile = activityPubClient
                .getActorProfile(webfinger.getActorUrl())
                .block();
        
        if (actorProfile == null) {
            throw new RuntimeException("Failed to retrieve actor profile: " + webfinger.getActorUrl());
        }
        
        // Extract actor information
        String actorId = actorProfile.has("id") ? actorProfile.get("id").asText() : webfinger.getActorUrl();
        String actorType = actorProfile.has("type") ? actorProfile.get("type").asText() : "Person";
        String inboxUrl = actorProfile.has("inbox") ? actorProfile.get("inbox").asText() : null;
        String outboxUrl = actorProfile.has("outbox") ? actorProfile.get("outbox").asText() : null;
        String sharedInboxUrl = actorProfile.has("sharedInbox") ? actorProfile.get("sharedInbox").asText() : null;
        
        // Convert JsonNode to Map for storage
        Map<String, Object> profileData = new HashMap<>();
        actorProfile.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) {
                profileData.put(entry.getKey(), entry.getValue().asText());
            } else if (entry.getValue().isNumber()) {
                profileData.put(entry.getKey(), entry.getValue().asLong());
            } else if (entry.getValue().isBoolean()) {
                profileData.put(entry.getKey(), entry.getValue().asBoolean());
            }
        });
        
        // Save or update actor
        ActivityPubActor actor;
        if (existingActor != null) {
            actor = existingActor;
        } else {
            actor = new ActivityPubActor();
            actor.setActorId(actorId);
            actor.setUsername(username);
            actor.setInstanceUrl(instanceUrl);
        }
        
        actor.setActorType(actorType);
        actor.setInboxUrl(inboxUrl);
        actor.setOutboxUrl(outboxUrl);
        actor.setSharedInboxUrl(sharedInboxUrl);
        actor.setProfileData(profileData);
        actor.setLastCheckedAt(LocalDateTime.now());
        
        actor = actorRepository.save(actor);
        log.info("Actor saved/updated: {}", actor.getActorId());
        
        return actor;
    }
    
    /**
     * Get actor by ID
     */
    public ActivityPubActor getActor(String actorId) {
        return actorRepository.findByActorId(actorId)
                .orElseThrow(() -> new RuntimeException("Actor not found: " + actorId));
    }

    /**
     * Get paginated activities for an actor (from collected_activities).
     */
    public PaginationResponse<CollectedActivityDTO> getActorActivities(String actorId, int page, int perPage) {
        Pageable pageable = PageRequest.of(page - 1, Math.min(perPage, 100));
        Page<CollectedActivity> p = collectedActivityRepository.findByActorIdOrderByPublishedAtDesc(actorId, pageable);
        List<CollectedActivityDTO> dtos = p.getContent().stream()
                .map(this::toCollectedActivityDTO)
                .collect(Collectors.toList());
        PaginationResponse.PaginationInfo info = PaginationResponse.PaginationInfo.builder()
                .page(page)
                .perPage(perPage)
                .total(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .build();
        return PaginationResponse.<CollectedActivityDTO>builder()
                .data(dtos)
                .pagination(info)
                .build();
    }

    private CollectedActivityDTO toCollectedActivityDTO(CollectedActivity a) {
        return CollectedActivityDTO.builder()
                .id(a.getId())
                .activityId(a.getActivityId())
                .activityType(a.getActivityType())
                .actorId(a.getActorId())
                .objectId(a.getObjectId())
                .objectType(a.getObjectType())
                .content(a.getContent())
                .publishedAt(a.getPublishedAt())
                .instanceUrl(a.getInstanceUrl())
                .monitorId(a.getMonitorId())
                .rawData(a.getRawData())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
