package com.activitypub.listener.activitypub;

import com.activitypub.listener.kafka.ActivityEventMessage;
import com.activitypub.listener.kafka.ActivityPubKafkaProducer;
import com.activitypub.listener.model.CollectedActivity;
import com.activitypub.listener.model.ActivityPubActor;
import com.activitypub.listener.repository.ActivityPubActorRepository;
import com.activitypub.listener.repository.CollectedActivityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Polls actor outboxes, parses ActivityStreams, persists activities and sends to Kafka.
 * Applies per-instance rate limiting and respects pagination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingService {

    @Value("${activitypub.outbox.max-pages-per-poll:5}")
    private int maxPagesPerPoll = 5;

    private final ActivityPubClient activityPubClient;
    private final ActivityStreamsParser activityStreamsParser;
    private final InstanceRateLimiter instanceRateLimiter;
    private final CollectedActivityRepository collectedActivityRepository;
    private final ActivityPubKafkaProducer kafkaProducer;
    private final ActivityPubActorRepository actorRepository;

    /**
     * Poll outbox for an actor by ID (looks up outbox URL from DB), parse, persist and send to Kafka.
     */
    public int pollActorOutbox(String actorId, String monitorId) {
        ActivityPubActor actor = actorRepository.findByActorId(actorId)
                .orElseThrow(() -> new IllegalArgumentException("Actor not found: " + actorId));
        return pollOutbox(actor.getOutboxUrl(), actor.getInstanceUrl(), monitorId);
    }

    /**
     * Poll outbox by URL. Parses all pages (up to maxPagesPerPoll), persists new activities and sends to Kafka.
     */
    public int pollOutbox(String outboxUrl, String instanceUrl, String monitorId) {
        if (outboxUrl == null || outboxUrl.isEmpty()) {
            log.warn("Outbox URL is null or empty");
            return 0;
        }
        String instance = instanceUrl != null ? instanceUrl : instanceRateLimiter.instanceFromUrl(outboxUrl);
        int totalCollected = 0;
        String currentUrl = outboxUrl.contains("?") ? outboxUrl : outboxUrl + "?page=true";
        int pages = 0;

        while (currentUrl != null && pages < maxPagesPerPoll) {
            try {
                instanceRateLimiter.acquire(currentUrl);
                JsonNode page = activityPubClient.getOutboxPage(currentUrl).block();
                if (page == null) break;

                List<ParsedActivity> parsed = activityStreamsParser.parseOutbox(page, instance);
                for (ParsedActivity p : parsed) {
                    if (saveAndPublish(p, monitorId)) {
                        totalCollected++;
                    }
                }

                currentUrl = activityStreamsParser.getNextPageUrl(page);
                pages++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Outbox polling interrupted");
                break;
            } catch (Exception e) {
                log.error("Error polling outbox {}: {}", currentUrl, e.getMessage());
                instanceRateLimiter.recordBackoff(currentUrl);
                break;
            }
        }

        log.info("Outbox poll completed: {} new activities from {} pages", totalCollected, pages);
        return totalCollected;
    }

    private boolean saveAndPublish(ParsedActivity p, String monitorId) {
        if (p.getActivityId() == null) return false;
        if (collectedActivityRepository.existsByActivityId(p.getActivityId())) {
            return false;
        }
        CollectedActivity entity = toCollectedActivity(p, monitorId);
        collectedActivityRepository.save(entity);
        kafkaProducer.sendActivityEvent(toEventMessage(p, monitorId));
        return true;
    }

    private static CollectedActivity toCollectedActivity(ParsedActivity p, String monitorId) {
        return CollectedActivity.builder()
                .activityId(p.getActivityId())
                .activityType(p.getActivityType())
                .actorId(p.getActorId())
                .objectId(p.getObjectId())
                .objectType(p.getObjectType())
                .content(p.getContent())
                .publishedAt(p.getPublishedAt())
                .instanceUrl(p.getInstanceUrl())
                .monitorId(monitorId)
                .rawData(p.getRawData())
                .build();
    }

    private static ActivityEventMessage toEventMessage(ParsedActivity p, String monitorId) {
        return ActivityEventMessage.builder()
                .activityId(p.getActivityId())
                .activityType(p.getActivityType())
                .actorId(p.getActorId())
                .objectId(p.getObjectId())
                .objectType(p.getObjectType())
                .content(p.getContent())
                .publishedAt(p.getPublishedAt())
                .instanceUrl(p.getInstanceUrl())
                .monitorId(monitorId)
                .rawData(p.getRawData())
                .build();
    }
}
