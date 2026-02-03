package com.activitypub.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityPubKafkaProducer {

    @Value("${kafka.topics.tracker-new:tracker-new}")
    private String trackerNewTopic;

    @Value("${kafka.topics.activities:activities}")
    private String activitiesTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendTrackerConfig(TrackerConfigMessage message) {
        String key = message.getTrackerId() != null ? message.getTrackerId() : "unknown";
        kafkaTemplate.send(trackerNewTopic, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send tracker config to {}: {}", trackerNewTopic, ex.getMessage());
                    } else {
                        log.debug("Sent tracker config to {}: {}", trackerNewTopic, key);
                    }
                });
    }

    public void sendActivityEvent(ActivityEventMessage message) {
        String key = message.getActivityId() != null ? message.getActivityId() : message.getActorId();
        if (key == null) key = "unknown";
        kafkaTemplate.send(activitiesTopic, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send activity to {}: {}", activitiesTopic, ex.getMessage());
                    } else {
                        log.debug("Sent activity to {}: {}", activitiesTopic, message.getActivityId());
                    }
                });
    }
}
