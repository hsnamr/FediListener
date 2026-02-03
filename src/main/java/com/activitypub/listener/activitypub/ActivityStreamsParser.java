package com.activitypub.listener.activitypub;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Parses ActivityStreams 2.0 JSON (Collection/OrderedCollection, activities).
 * Handles Create, Update, Delete, Announce, Like, Follow, Undo and extracts
 * object types Note, Article, Video, Comment, etc.
 */
@Component
@Slf4j
public class ActivityStreamsParser {

    private static final Set<String> ACTIVITY_TYPES = Set.of(
            "Create", "Update", "Delete", "Announce", "Like", "Follow", "Undo",
            "Accept", "Reject", "Add", "Remove", "Block", "Flag"
    );

    private static final Set<String> OBJECT_TYPES = Set.of(
            "Note", "Article", "Video", "Image", "Comment", "Document"
    );

    /**
     * Parse an outbox/collection response into a list of parsed activities.
     * Handles both "first" page (with "orderedItems" or "items") and inline "orderedItems"/"items".
     */
    public List<ParsedActivity> parseOutbox(JsonNode root, String instanceUrl) {
        List<JsonNode> items = extractItems(root);
        List<ParsedActivity> result = new ArrayList<>();
        for (JsonNode item : items) {
            try {
                ParsedActivity parsed = parseActivityItem(item, instanceUrl);
                if (parsed != null) {
                    result.add(parsed);
                }
            } catch (Exception e) {
                log.warn("Failed to parse activity item: {}", e.getMessage());
            }
        }
        return result;
    }

    /**
     * Extract items from Collection or OrderedCollection.
     * Supports: orderedItems, items, or "first" as URL (caller should fetch separately).
     */
    public List<JsonNode> extractItems(JsonNode root) {
        if (root.has("orderedItems")) {
            return toList(root.get("orderedItems"));
        }
        if (root.has("items")) {
            return toList(root.get("items"));
        }
        if (root.has("first")) {
            JsonNode first = root.get("first");
            if (first.isObject() && first.has("orderedItems")) {
                return toList(first.get("orderedItems"));
            }
            if (first.isObject() && first.has("items")) {
                return toList(first.get("items"));
            }
        }
        return Collections.emptyList();
    }

    /**
     * Get the "next" page URL if present (for OrderedCollection pagination).
     */
    public String getNextPageUrl(JsonNode root) {
        if (root.has("next")) {
            return root.get("next").asText(null);
        }
        if (root.has("first") && root.get("first").isTextual()) {
            return null;
        }
        JsonNode first = root.has("first") ? root.get("first") : null;
        if (first != null && first.isObject() && first.has("next")) {
            return first.get("next").asText(null);
        }
        return null;
    }

    public ParsedActivity parseActivityItem(JsonNode item, String instanceUrl) {
        if (item == null || !item.isObject()) {
            return null;
        }
        String type = item.has("type") ? item.get("type").asText() : null;
        if (type == null) {
            return null;
        }

        String activityId = item.has("id") ? item.get("id").asText() : null;
        String actorId = item.has("actor") ? asId(item.get("actor")) : null;
        String objectId = null;
        String objectType = null;
        String content = null;
        LocalDateTime publishedAt = null;

        JsonNode objectNode = item.has("object") ? item.get("object") : null;
        if (objectNode != null && objectNode.isObject()) {
            objectId = objectNode.has("id") ? objectNode.get("id").asText() : null;
            objectType = objectNode.has("type") ? objectNode.get("type").asText() : null;
            if (objectNode.has("content")) {
                content = objectNode.get("content").asText(null);
            }
            if (objectNode.has("published")) {
                publishedAt = parseTimestamp(objectNode.get("published").asText(null));
            }
        }
        if (publishedAt == null && item.has("published")) {
            publishedAt = parseTimestamp(item.get("published").asText(null));
        }

        Map<String, Object> rawData = new HashMap<>();
        item.fields().forEachRemaining(entry -> putJsonValue(rawData, entry.getKey(), entry.getValue()));

        return ParsedActivity.builder()
                .activityId(activityId)
                .activityType(ACTIVITY_TYPES.contains(type) ? type : type)
                .actorId(actorId)
                .objectId(objectId)
                .objectType(objectType != null ? objectType : "Object")
                .content(content)
                .publishedAt(publishedAt)
                .instanceUrl(instanceUrl)
                .rawData(rawData)
                .build();
    }

    private static String asId(JsonNode node) {
        if (node == null) return null;
        return node.isTextual() ? node.asText() : (node.has("id") ? node.get("id").asText() : null);
    }

    private static LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<JsonNode> toList(JsonNode array) {
        if (array == null || !array.isArray()) return Collections.emptyList();
        List<JsonNode> list = new ArrayList<>();
        array.forEach(list::add);
        return list;
    }

    private static void putJsonValue(Map<String, Object> map, String key, JsonNode value) {
        if (value == null) return;
        if (value.isTextual()) map.put(key, value.asText());
        else if (value.isNumber()) map.put(key, value.numberValue());
        else if (value.isBoolean()) map.put(key, value.asBoolean());
        else if (value.isArray() || value.isObject()) map.put(key, value.toString());
    }
}
