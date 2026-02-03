package com.activitypub.listener.service;

import com.activitypub.listener.dto.CreateFediverseInstanceDTO;
import com.activitypub.listener.dto.FediverseInstanceDTO;
import com.activitypub.listener.exception.ResourceNotFoundException;
import com.activitypub.listener.model.FediverseInstance;
import com.activitypub.listener.repository.FediverseInstanceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstanceService {

    @Value("${activitypub.request-timeout:30000}")
    private int requestTimeout;

    private final FediverseInstanceRepository instanceRepository;
    private final WebClient.Builder webClientBuilder;

    public List<FediverseInstanceDTO> listAll() {
        return instanceRepository.findByIsActiveTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Page<FediverseInstanceDTO> list(Pageable pageable) {
        return instanceRepository.findAll(pageable).map(this::toDTO);
    }

    public FediverseInstanceDTO getById(String id) {
        FediverseInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found: " + id));
        return toDTO(instance);
    }

    public FediverseInstanceDTO create(CreateFediverseInstanceDTO dto) {
        String url = normalizeInstanceUrl(dto.getInstanceUrl());
        if (instanceRepository.findByInstanceUrl(url).isPresent()) {
            throw new IllegalStateException("Instance already registered: " + url);
        }
        FediverseInstance instance = FediverseInstance.builder()
                .instanceUrl(url)
                .instanceType(dto.getInstanceType() != null ? dto.getInstanceType() : "mastodon")
                .nodeinfoUrl(url + "/.well-known/nodeinfo")
                .rateLimitPerMinute(dto.getRateLimitPerMinute() != null ? dto.getRateLimitPerMinute() : 300)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .healthStatus("unknown")
                .build();
        instance = instanceRepository.save(instance);
        log.info("Created fediverse instance: {}", instance.getInstanceUrl());
        return toDTO(instance);
    }

    public FediverseInstanceDTO update(String id, CreateFediverseInstanceDTO dto) {
        FediverseInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found: " + id));
        if (dto.getInstanceType() != null) instance.setInstanceType(dto.getInstanceType());
        if (dto.getRateLimitPerMinute() != null) instance.setRateLimitPerMinute(dto.getRateLimitPerMinute());
        if (dto.getIsActive() != null) instance.setIsActive(dto.getIsActive());
        instance = instanceRepository.save(instance);
        return toDTO(instance);
    }

    public void delete(String id) {
        if (!instanceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Instance not found: " + id);
        }
        instanceRepository.deleteById(id);
        log.info("Deleted fediverse instance: {}", id);
    }

    /**
     * Check instance health (NodeInfo or root URL) and update stored status.
     */
    public Map<String, Object> healthCheck(String id) {
        FediverseInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found: " + id));
        String status = "down";
        String message = null;
        try {
            JsonNode nodeInfo = webClientBuilder.build()
                    .get()
                    .uri(instance.getInstanceUrl() + "/.well-known/nodeinfo")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofMillis(requestTimeout))
                    .block();
            if (nodeInfo != null && nodeInfo.has("links")) {
                status = "healthy";
            } else {
                status = "degraded";
                message = "NodeInfo missing or empty";
            }
        } catch (Exception e) {
            message = e.getMessage();
            log.warn("Health check failed for {}: {}", instance.getInstanceUrl(), e.getMessage());
        }
        instance.setLastHealthCheck(LocalDateTime.now());
        instance.setHealthStatus(status);
        instanceRepository.save(instance);
        return Map.of(
                "status", status,
                "instanceUrl", instance.getInstanceUrl(),
                "lastChecked", instance.getLastHealthCheck(),
                "message", message != null ? message : ""
        );
    }

    private FediverseInstanceDTO toDTO(FediverseInstance e) {
        return FediverseInstanceDTO.builder()
                .id(e.getId())
                .instanceUrl(e.getInstanceUrl())
                .instanceType(e.getInstanceType())
                .nodeinfoUrl(e.getNodeinfoUrl())
                .rateLimitPerMinute(e.getRateLimitPerMinute())
                .isActive(e.getIsActive())
                .healthStatus(e.getHealthStatus())
                .lastHealthCheck(e.getLastHealthCheck())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private static String normalizeInstanceUrl(String url) {
        if (url == null) return null;
        url = url.trim().toLowerCase();
        if (!url.startsWith("http")) url = "https://" + url;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }
}
