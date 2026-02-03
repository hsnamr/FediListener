package com.activitypub.listener.mapper;

import com.activitypub.listener.dto.MonitorDTO;
import com.activitypub.listener.model.DataSource;
import com.activitypub.listener.model.Monitor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MonitorMapper {

    public MonitorDTO toDTO(Monitor monitor) {
        if (monitor == null) {
            return null;
        }

        List<MonitorDTO.DataSourceDTO> dataSources = collectDataSources(monitor);

        return MonitorDTO.builder()
                .id(monitor.getId())
                .name(monitor.getName())
                .monitorType(monitor.getMonitorType() != null ?
                        MonitorDTO.MonitorTypeDTO.builder()
                                .id(monitor.getMonitorType().getId())
                                .name(monitor.getMonitorType().getName())
                                .build() : null)
                .companyId(null)
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
                .dataSources(dataSources)
                .createdAt(monitor.getCreatedAt())
                .updatedAt(monitor.getUpdatedAt())
                .build();
    }

    private List<MonitorDTO.DataSourceDTO> collectDataSources(Monitor monitor) {
        Set<String> seenIds = new LinkedHashSet<>();
        List<MonitorDTO.DataSourceDTO> list = new ArrayList<>();
        Stream<DataSource> fromKeywords = monitor.getKeywords() != null ? monitor.getKeywords().stream().map(k -> k.getDataSource()) : Stream.empty();
        Stream<DataSource> fromAccounts = monitor.getAccountAnalyses() != null ? monitor.getAccountAnalyses().stream().map(a -> a.getDataSource()) : Stream.empty();
        Stream<DataSource> fromRegionals = monitor.getRegionals() != null ? monitor.getRegionals().stream().map(r -> r.getDataSource()) : Stream.empty();
        Stream<DataSource> fromManaged = monitor.getManagedAccounts() != null ? monitor.getManagedAccounts().stream().map(m -> m.getDataSource()) : Stream.empty();
        Stream.concat(Stream.concat(Stream.concat(fromKeywords, fromAccounts), fromRegionals), fromManaged)
                .filter(ds -> ds != null && seenIds.add(ds.getId()))
                .forEach(ds -> list.add(MonitorDTO.DataSourceDTO.builder()
                        .id(ds.getId())
                        .source(ds.getSource())
                        .category(ds.getCategory())
                        .build()));
        return list;
    }
}
