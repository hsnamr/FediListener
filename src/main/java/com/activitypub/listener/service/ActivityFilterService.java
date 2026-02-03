package com.activitypub.listener.service;

import com.activitypub.listener.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Determines whether a collected activity matches a monitor's rules
 * (keyword, account analysis, regional). §6.2.1–6.2.3.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityFilterService {

    /**
     * Returns true if the activity matches any of the monitor's rules
     * (keyword match, account follow, or regional if applicable).
     */
    public boolean matchesMonitor(Monitor monitor, CollectedActivity activity) {
        if (monitor == null || activity == null) return false;

        if (matchesKeywordMonitor(monitor, activity)) return true;
        if (matchesAccountAnalysisMonitor(monitor, activity)) return true;
        if (matchesRegionalMonitor(monitor, activity)) return true;

        return false;
    }

    /**
     * Keyword monitor: activity content (or object content) contains any keyword
     * and does not contain spam keywords. Keywords are matched case-insensitively.
     */
    public boolean matchesKeywordMonitor(Monitor monitor, CollectedActivity activity) {
        if (monitor.getKeywords() == null || monitor.getKeywords().isEmpty()) return false;

        String content = activity.getContent();
        if (content == null) content = "";

        for (Keyword k : monitor.getKeywords()) {
            if (!Boolean.TRUE.equals(k.getIsActive())) continue;
            Set<String> keywords = parseKeywords(k.getKeywords());
            Set<String> spamKeywords = parseKeywords(k.getSpamKeywords());
            if (keywords.isEmpty()) continue;
            if (containsAny(content, spamKeywords)) continue;
            if (containsAny(content, keywords)) return true;
        }
        return false;
    }

    /**
     * Account analysis: activity actor matches a "follow" account identifier.
     * Excluded accounts are skipped.
     */
    public boolean matchesAccountAnalysisMonitor(Monitor monitor, CollectedActivity activity) {
        if (monitor.getAccountAnalyses() == null || monitor.getAccountAnalyses().isEmpty()) return false;
        String actorId = activity.getActorId();
        if (actorId == null) return false;

        for (AccountAnalysis a : monitor.getAccountAnalyses()) {
            String follow = a.getFollow();
            if (follow == null || follow.isEmpty()) continue;
            String normalizedFollow = normalizeAccountIdentifier(follow);
            if (actorMatches(actorId, normalizedFollow)) {
                if (isExcluded(actorId, a.getExcludedAccounts())) continue;
                return true;
            }
        }
        return false;
    }

    /**
     * Regional monitor: if activity has location data and monitor has MBR, check containment.
     * Many ActivityPub activities do not have geographic data; we accept if no MBR or no location.
     */
    public boolean matchesRegionalMonitor(Monitor monitor, CollectedActivity activity) {
        if (monitor.getRegionals() == null || monitor.getRegionals().isEmpty()) return false;
        for (Regional r : monitor.getRegionals()) {
            if (r.getMbr() == null || r.getMbr().isEmpty()) return true;
            // Location data rarely present in ActivityStreams; if we had lat/lon we would check MBR here.
            // For now we do not have location on CollectedActivity, so regional match is not applied.
        }
        return false;
    }

    private static Set<String> parseKeywords(String raw) {
        if (raw == null || raw.isEmpty()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static boolean containsAny(String content, Set<String> terms) {
        if (content == null || terms == null || terms.isEmpty()) return false;
        String lower = content.toLowerCase();
        return terms.stream().anyMatch(lower::contains);
    }

    private static String normalizeAccountIdentifier(String follow) {
        if (follow == null) return "";
        follow = follow.trim();
        if (follow.startsWith("acct:")) follow = follow.substring(5);
        if (follow.startsWith("@")) follow = follow.substring(1);
        return follow.toLowerCase();
    }

    private static boolean actorMatches(String actorId, String normalizedFollow) {
        if (actorId == null || normalizedFollow.isEmpty()) return false;
        String lower = actorId.toLowerCase();
        return lower.contains(normalizedFollow) || lower.endsWith("/" + normalizedFollow);
    }

    private static boolean isExcluded(String actorId, String excludedAccounts) {
        if (excludedAccounts == null || excludedAccounts.isEmpty()) return false;
        Set<String> excluded = Arrays.stream(excludedAccounts.split(","))
                .map(String::trim)
                .map(ActivityFilterService::normalizeAccountIdentifier)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        String normalized = normalizeAccountIdentifier(actorId);
        return excluded.stream().anyMatch(normalized::contains);
    }
}
