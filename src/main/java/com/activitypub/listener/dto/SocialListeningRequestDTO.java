package com.activitypub.listener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request for social listening data. §3.9, §4.4.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialListeningRequestDTO {

    @NotBlank(message = "monitor_id is required")
    private String monitorId;

    @NotBlank(message = "data_source is required")
    private String dataSource;

    @NotBlank(message = "page_name is required")
    private String pageName;

    @NotNull(message = "start_date is required")
    private Long startDate;

    @NotNull(message = "end_date is required")
    private Long endDate;

    private SocialListeningFiltersDTO filters;

    private List<String> widgetsNames;

    @Builder.Default
    private Integer pageNumber = 1;
}
