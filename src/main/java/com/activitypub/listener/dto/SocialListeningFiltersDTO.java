package com.activitypub.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Filters for social listening request. §6.5.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialListeningFiltersDTO {
    private List<Long> topics;
    private List<String> sentiment;
    private List<String> languages;
    private List<String> users;
    private List<String> excludeUser;
    private String ownerUsername;
    private String excludeOwnerUsername;
    private List<Long> accountLists;
    private List<Long> excludeAccountLists;
    private List<Long> manualTopics;
}
