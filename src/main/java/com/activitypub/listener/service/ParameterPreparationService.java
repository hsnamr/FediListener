package com.activitypub.listener.service;

import com.activitypub.listener.dto.SocialListeningFiltersDTO;
import com.activitypub.listener.dto.SocialListeningRequestDTO;
import com.activitypub.listener.kafka.SocialListeningAnalyticsMessage;
import com.activitypub.listener.model.Monitor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Prepares parameters for analytics engine. §6.4, §7.1.2.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParameterPreparationService {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/activitypub_listener}")
    private String mongodbUri;

    @Value("${spring.data.mongodb.database:activitypub_listener}")
    private String mongodbDatabase;

    @Value("${kafka.consumer-groups.frontend:frontend-consumer}")
    private String frontendConsumerGroupPrefix;

    private final ObjectMapper objectMapper;

    /**
     * Build analytics engine message from request and monitor.
     * Topic/consumer group naming: §7.1.3.
     */
    public SocialListeningAnalyticsMessage prepare(SocialListeningRequestDTO request, Monitor monitor) {
        String topic = buildTopicName(request.getPageName(), monitor.getId());
        String consumerGroup = buildConsumerGroupName(request.getPageName(), monitor.getId());
        String routingKey = "account_data_monitor_" + monitor.getId() + "_" + request.getStartDate();

        String dbHostname = extractHostname(mongodbUri);
        String filtersJson = serializeFilters(request.getFilters());

        String monitorTypeName = monitor.getMonitorType() != null ? monitor.getMonitorType().getName() : null;
        List<String> widgetsNames = request.getWidgetsNames() != null ? request.getWidgetsNames() : List.of();

        return SocialListeningAnalyticsMessage.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .monitorId(monitor.getId())
                .dataSourceName(request.getDataSource())
                .userId(monitor.getUserId())
                .dbHostname(dbHostname)
                .dbName(mongodbDatabase)
                .routingKey(routingKey)
                .topic(topic)
                .consumerGroup(consumerGroup)
                .filters(filtersJson)
                .pageName(request.getPageName())
                .monitorType(monitorTypeName)
                .widgetsNames(widgetsNames)
                .pageNumber(request.getPageNumber() != null ? request.getPageNumber() : 1)
                .build();
    }

    private String buildTopicName(String pageName, String monitorId) {
        return pageName + "-data-monitor-" + monitorId;
    }

    private String buildConsumerGroupName(String pageName, String monitorId) {
        return frontendConsumerGroupPrefix + "-" + pageName + "-monitor-" + monitorId;
    }

    private static String extractHostname(String uri) {
        if (uri == null) return "localhost";
        try {
            int start = uri.indexOf("://");
            if (start < 0) return "localhost";
            int end = uri.indexOf("/", start + 3);
            if (end < 0) end = uri.length();
            String hostPart = uri.substring(start + 3, end);
            if (hostPart.contains("@")) hostPart = hostPart.substring(hostPart.indexOf("@") + 1);
            if (hostPart.contains(":")) hostPart = hostPart.substring(0, hostPart.indexOf(":"));
            return hostPart.isEmpty() ? "localhost" : hostPart;
        } catch (Exception e) {
            return "localhost";
        }
    }

    private String serializeFilters(SocialListeningFiltersDTO filters) {
        if (filters == null) return "{}";
        try {
            return objectMapper.writeValueAsString(filters);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize filters: {}", e.getMessage());
            return "{}";
        }
    }
}
