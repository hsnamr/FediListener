package com.activitypub.listener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMonitorDTO {
    private String name;
    private String monitorTypeId;
    private Long productId;
    private List<String> dataSources;
    private Map<String, Map<String, Map<String, Object>>> monitorOptions;
    private String languages;
    private Boolean exactSearch;
    private List<String> customerCareAccounts;
    private Boolean autoSummaryWidget;
}
