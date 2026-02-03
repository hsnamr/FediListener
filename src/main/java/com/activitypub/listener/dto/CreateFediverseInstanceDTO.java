package com.activitypub.listener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFediverseInstanceDTO {
    @NotBlank(message = "Instance URL is required")
    private String instanceUrl;
    private String instanceType;
    private Integer rateLimitPerMinute;
    private Boolean isActive;
}
