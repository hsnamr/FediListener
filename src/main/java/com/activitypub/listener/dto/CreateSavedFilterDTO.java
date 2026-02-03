package com.activitypub.listener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSavedFilterDTO {
    @NotBlank(message = "Filter name is required")
    private String name;

    @NotNull(message = "Filters are required")
    private Map<String, Object> filters;
}
