package com.activitypub.listener.dto;

import com.activitypub.listener.model.Monitor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorDTO {
    private String id;
    private String name;
    private MonitorTypeDTO monitorType;
    private Long companyId;
    private Long userId;
    private Long productId;
    private Boolean isDeleted;
    private Boolean paused;
    private String isApproved;
    private String monitorFeatureType;
    private String languages;
    private Boolean exactSearch;
    private Boolean autoSummaryWidget;
    private Integer version;
    private List<DataSourceDTO> dataSources;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitorTypeDTO {
        private String id;
        private String name;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceDTO {
        private String id;
        private String source;
        private String category;
    }
}
