package com.activitypub.listener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
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
public class CreateMonitorDTO {
    
    @NotBlank(message = "Monitor name is required")
    private String name;
    
    @NotBlank(message = "Monitor type ID is required")
    private String monitorTypeId;
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotEmpty(message = "At least one data source is required")
    private List<String> dataSources;
    
    private Map<String, Map<String, Map<String, Object>>> monitorOptions;
    
    private String languages;
    
    private Boolean exactSearch;
    
    private List<String> customerCareAccounts;
    
    private Boolean autoSummaryWidget;
}
