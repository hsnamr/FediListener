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

    @Value("${kafka.topics.monitor-lifecycle:monitor-lifecycle}")
    private String monitorLifecycleTopic;

    @Value("${kafka.topics.activities:activities}")
    private String activitiesTopic;

    @Value("${kafka.topics.social-listening:social-listening}")
    private String socialListeningTopic;

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

    public void sendSocialListeningRequest(SocialListeningAnalyticsMessage message) {
        String key = message.getMonitorId() != null ? message.getMonitorId() : "unknown";
        kafkaTemplate.send(socialListeningTopic, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send social listening request to {}: {}", socialListeningTopic, ex.getMessage());
                    } else {
                        log.debug("Sent social listening request to {}: {}", socialListeningTopic, key);
                    }
                });
    }

    public void sendMonitorLifecycle(MonitorLifecycleMessage message) {
        String key = message.getMonitorId() != null ? message.getMonitorId() : "unknown";
        kafkaTemplate.send(monitorLifecycleTopic, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send monitor lifecycle to {}: {}", monitorLifecycleTopic, ex.getMessage());
                    } else {
                        log.debug("Sent monitor lifecycle to {}: {} {}", monitorLifecycleTopic, message.getMonitorId(), message.getAction());
                    }
                });
    }
}
