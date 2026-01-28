package com.activitypub.listener.mapper;

import com.activitypub.listener.dto.MonitorDTO;
import com.activitypub.listener.model.DataSource;
import com.activitypub.listener.model.Monitor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class MonitorMapper {
    
    public MonitorDTO toDTO(Monitor monitor) {
        if (monitor == null) {
            return null;
        }
        
        return MonitorDTO.builder()
                .id(monitor.getId())
                .name(monitor.getName())
                .monitorType(monitor.getMonitorType() != null ? 
                    MonitorDTO.MonitorTypeDTO.builder()
                        .id(monitor.getMonitorType().getId())
                        .name(monitor.getMonitorType().getName())
                        .build() : null)
                .userId(monitor.getUserId())
                .productId(monitor.getProductId())
                .isDeleted(monitor.getIsDeleted())
                .paused(monitor.getPaused())
                .isApproved(monitor.getIsApproved() != null ? monitor.getIsApproved().name() : null)
                .monitorFeatureType(monitor.getMonitorFeatureType() != null ? monitor.getMonitorFeatureType().name() : null)
                .languages(monitor.getLanguages())
                .exactSearch(monitor.getExactSearch())
                .autoSummaryWidget(monitor.getAutoSummaryWidget())
                .version(monitor.getVersion())
                .dataSources(monitor.getKeywords().stream()
                    .map(k -> k.getDataSource())
                    .distinct()
                    .map(ds -> MonitorDTO.DataSourceDTO.builder()
                        .id(ds.getId())
                        .source(ds.getSource())
                        .category(ds.getCategory())
                        .build())
                    .collect(Collectors.toList()))
                .createdAt(monitor.getCreatedAt())
                .updatedAt(monitor.getUpdatedAt())
                .build();
    }
}
