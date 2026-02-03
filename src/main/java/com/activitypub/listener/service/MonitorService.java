package com.activitypub.listener.service;

import com.activitypub.listener.dto.CollectedActivityDTO;
import com.activitypub.listener.dto.CreateMonitorDTO;
import com.activitypub.listener.dto.MonitorDTO;
import com.activitypub.listener.dto.PaginationResponse;
import com.activitypub.listener.dto.UpdateMonitorDTO;
import com.activitypub.listener.exception.ResourceNotFoundException;
import com.activitypub.listener.kafka.ActivityPubKafkaProducer;
import com.activitypub.listener.kafka.MonitorLifecycleMessage;
import com.activitypub.listener.kafka.TrackerConfigMessage;
import com.activitypub.listener.mapper.MonitorMapper;
import com.activitypub.listener.model.CollectedActivity;
import com.activitypub.listener.model.*;
import com.activitypub.listener.repository.CollectedActivityRepository;
import com.activitypub.listener.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorService {
    
    private final MonitorRepository monitorRepository;
    private final MonitorTypeRepository monitorTypeRepository;
    private final DataSourceRepository dataSourceRepository;
    private final KeywordRepository keywordRepository;
    private final MonitorMapper monitorMapper;
    private final ActivityPubKafkaProducer kafkaProducer;
    private final CollectedActivityRepository collectedActivityRepository;

    @Value("${social.listening.max-monitors-per-user:100}")
    private int maxMonitorsPerUser;

    public MonitorDTO createMonitor(CreateMonitorDTO dto, Long userId) {
        log.info("Creating monitor: {} for user: {}", dto.getName(), userId);

        if (userId != null) {
            long count = monitorRepository.countByUserIdAndIsDeletedFalse(userId);
            if (count >= maxMonitorsPerUser) {
                throw new IllegalStateException("Monitor limit exceeded (max: " + maxMonitorsPerUser + " per user)");
            }
        }

        MonitorType monitorType = monitorTypeRepository.findById(dto.getMonitorTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Monitor type not found: " + dto.getMonitorTypeId()));
        
        List<DataSource> dataSources = dataSourceRepository.findAllById(dto.getDataSources());
        if (dataSources.size() != dto.getDataSources().size()) {
            throw new ResourceNotFoundException("One or more data sources not found");
        }
        
        Monitor monitor = Monitor.builder()
                .name(dto.getName())
                .monitorType(monitorType)
                .userId(userId)
                .creatorId(userId)
                .productId(dto.getProductId())
                .isApproved(Monitor.ApprovalStatus.UNAPPROVED)
                .languages(dto.getLanguages())
                .exactSearch(dto.getExactSearch() != null ? dto.getExactSearch() : false)
                .autoSummaryWidget(dto.getAutoSummaryWidget() != null ? dto.getAutoSummaryWidget() : false)
                .build();
        
        // Create monitor type-specific records (embedded documents)
        if (dto.getMonitorOptions() != null) {
            createMonitorOptions(monitor, dto.getMonitorOptions(), dataSources);
        }
        
        monitor = monitorRepository.save(monitor);
        sendTrackerConfigToKafka(monitor);
        log.info("Monitor created successfully with ID: {}", monitor.getId());
        return monitorMapper.toDTO(monitor);
    }
    
    public PaginationResponse<MonitorDTO> listMonitors(
            Pageable pageable,
            String search,
            String monitorTypeId,
            Long productId,
            String dataSourceId,
            Monitor.ApprovalStatus isApproved,
            Boolean paused,
            Long userId,
            String sortBy,
            String orderBy) {
        Page<Monitor> monitors = monitorRepository.findMonitors(
                search, monitorTypeId, productId, dataSourceId, isApproved, paused, userId,
                sortBy, orderBy, pageable);

        List<MonitorDTO> monitorDTOs = monitors.getContent().stream()
                .map(monitorMapper::toDTO)
                .collect(Collectors.toList());

        PaginationResponse.PaginationInfo paginationInfo = PaginationResponse.PaginationInfo.builder()
                .page(monitors.getNumber() + 1)
                .perPage(monitors.getSize())
                .total(monitors.getTotalElements())
                .totalPages(monitors.getTotalPages())
                .build();

        return PaginationResponse.<MonitorDTO>builder()
                .data(monitorDTOs)
                .pagination(paginationInfo)
                .build();
    }
    
    public MonitorDTO getMonitor(String id, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + id));
        ensureOwnership(monitor, userId);
        return monitorMapper.toDTO(monitor);
    }

    private void ensureOwnership(Monitor monitor, Long userId) {
        if (userId != null && monitor.getUserId() != null && !monitor.getUserId().equals(userId)) {
            throw new IllegalStateException("Monitor does not belong to user");
        }
    }

    public MonitorDTO updateMonitor(String id, UpdateMonitorDTO dto, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + id));
        ensureOwnership(monitor, userId);
        
        if (dto.getName() != null) {
            monitor.setName(dto.getName());
        }
        if (dto.getLanguages() != null) {
            monitor.setLanguages(dto.getLanguages());
        }
        if (dto.getExactSearch() != null) {
            monitor.setExactSearch(dto.getExactSearch());
        }
        if (dto.getAutoSummaryWidget() != null) {
            monitor.setAutoSummaryWidget(dto.getAutoSummaryWidget());
        }
        
        if (dto.getMonitorTypeId() != null) {
            MonitorType monitorType = monitorTypeRepository.findById(dto.getMonitorTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Monitor type not found: " + dto.getMonitorTypeId()));
            monitor.setMonitorType(monitorType);
        }
        
        if (dto.getMonitorOptions() != null && dto.getDataSources() != null) {
            // Remove existing options
            monitor.getKeywords().clear();
            monitor.getAccountAnalyses().clear();
            monitor.getRegionals().clear();
            if (monitor.getManagedAccounts() != null) monitor.getManagedAccounts().clear();

            List<DataSource> dataSources = dataSourceRepository.findAllById(dto.getDataSources());
            createMonitorOptions(monitor, dto.getMonitorOptions(), dataSources);
        }
        
        monitor = monitorRepository.save(monitor);
        sendTrackerConfigToKafka(monitor);
        log.info("Monitor updated successfully: {}", id);
        return monitorMapper.toDTO(monitor);
    }

    private void sendTrackerConfigToKafka(Monitor monitor) {
        String monitorTypeName = monitor.getMonitorType() != null ? monitor.getMonitorType().getName() : "KEYWORD";
        for (Keyword k : monitor.getKeywords()) {
            TrackerConfigMessage msg = TrackerConfigMessage.builder()
                    .trackerId(monitor.getId())
                    .dataSourceName(k.getDataSource() != null ? k.getDataSource().getSource() : null)
                    .monitorType(monitorTypeName)
                    .keywords(k.getKeywords())
                    .spamKeywords(k.getSpamKeywords())
                    .shouldCollect(!Boolean.TRUE.equals(monitor.getPaused()) && monitor.getIsApproved() == Monitor.ApprovalStatus.APPROVED)
                    .userId(monitor.getUserId())
                    .build();
            kafkaProducer.sendTrackerConfig(msg);
        }
        for (AccountAnalysis a : monitor.getAccountAnalyses()) {
            TrackerConfigMessage msg = TrackerConfigMessage.builder()
                    .trackerId(monitor.getId())
                    .dataSourceName(a.getDataSource() != null ? a.getDataSource().getSource() : null)
                    .monitorType(monitorTypeName)
                    .shouldCollect(!Boolean.TRUE.equals(monitor.getPaused()) && monitor.getIsApproved() == Monitor.ApprovalStatus.APPROVED)
                    .userId(monitor.getUserId())
                    .extra(a.getFollow() != null ? Map.of("follow", a.getFollow()) : null)
                    .build();
            kafkaProducer.sendTrackerConfig(msg);
        }
    }
    
    public void deleteMonitor(String id, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + id));
        ensureOwnership(monitor, userId);
        monitor.setIsDeleted(true);
        monitorRepository.save(monitor);
        log.info("Monitor soft deleted: {}", id);
    }
    
    public MonitorDTO pauseMonitor(String id, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + id));
        ensureOwnership(monitor, userId);
        monitor.setPaused(true);
        monitor = monitorRepository.save(monitor);
        log.info("Monitor paused: {}", id);
        
        return monitorMapper.toDTO(monitor);
    }
    
    public MonitorDTO resumeMonitor(String id) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + id));

        if (monitor.getIsApproved() != Monitor.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Monitor must be approved before resuming");
        }

        monitor.setPaused(false);
        monitor = monitorRepository.save(monitor);
        log.info("Monitor resumed: {}", id);

        return monitorMapper.toDTO(monitor);
    }

    public MonitorDTO approveMonitor(String id, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + id));
        if (userId != null) ensureOwnership(monitor, userId);

        Monitor.ApprovalStatus previous = monitor.getIsApproved();
        if (previous == Monitor.ApprovalStatus.APPROVED) {
            return monitorMapper.toDTO(monitor);
        }

        monitor.setIsApproved(Monitor.ApprovalStatus.APPROVED);
        monitor = monitorRepository.save(monitor);
        sendTrackerConfigToKafka(monitor);

        kafkaProducer.sendMonitorLifecycle(MonitorLifecycleMessage.builder()
                .monitorId(monitor.getId())
                .action("APPROVED")
                .previousStatus(previous != null ? previous.name() : null)
                .newStatus(Monitor.ApprovalStatus.APPROVED.name())
                .userId(monitor.getUserId())
                .at(LocalDateTime.now())
                .build());
        log.info("Monitor approved: {}", id);
        return monitorMapper.toDTO(monitor);
    }

    public PaginationResponse<CollectedActivityDTO> getActivitiesForMonitor(String monitorId, int page, int perPage, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + monitorId));
        ensureOwnership(monitor, userId);
        Pageable pageable = PageRequest.of(page - 1, Math.min(perPage, 100));
        Page<CollectedActivity> p = collectedActivityRepository.findByMonitorIdOrderByPublishedAtDesc(monitorId, pageable);
        List<CollectedActivityDTO> dtos = p.getContent().stream()
                .map(this::toCollectedActivityDTO)
                .collect(Collectors.toList());
        PaginationResponse.PaginationInfo info = PaginationResponse.PaginationInfo.builder()
                .page(page)
                .perPage(perPage)
                .total(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .build();
        return PaginationResponse.<CollectedActivityDTO>builder()
                .data(dtos)
                .pagination(info)
                .build();
    }

    private CollectedActivityDTO toCollectedActivityDTO(CollectedActivity a) {
        return CollectedActivityDTO.builder()
                .id(a.getId())
                .activityId(a.getActivityId())
                .activityType(a.getActivityType())
                .actorId(a.getActorId())
                .objectId(a.getObjectId())
                .objectType(a.getObjectType())
                .content(a.getContent())
                .publishedAt(a.getPublishedAt())
                .instanceUrl(a.getInstanceUrl())
                .monitorId(a.getMonitorId())
                .rawData(a.getRawData())
                .createdAt(a.getCreatedAt())
                .build();
    }

    public MonitorDTO rejectMonitor(String id, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + id));
        if (userId != null) ensureOwnership(monitor, userId);
        Monitor.ApprovalStatus previous = monitor.getIsApproved();
        boolean wasApproved = previous == Monitor.ApprovalStatus.APPROVED;
        Monitor.ApprovalStatus newStatus = wasApproved ? Monitor.ApprovalStatus.APPROVED_REJECTED : Monitor.ApprovalStatus.UNAPPROVED_REJECTED;

        monitor.setIsApproved(newStatus);
        monitor = monitorRepository.save(monitor);

        kafkaProducer.sendMonitorLifecycle(MonitorLifecycleMessage.builder()
                .monitorId(monitor.getId())
                .action("REJECTED")
                .previousStatus(previous != null ? previous.name() : null)
                .newStatus(newStatus.name())
                .userId(monitor.getUserId())
                .at(LocalDateTime.now())
                .build());
        log.info("Monitor rejected: {}", id);
        return monitorMapper.toDTO(monitor);
    }

    private void createMonitorOptions(Monitor monitor, Map<String, Map<String, Map<String, Object>>> monitorOptions, List<DataSource> dataSources) {
        // Handle KEYWORD monitor type
        if (monitorOptions.containsKey("KEYWORD")) {
            Map<String, Map<String, Object>> keywordOptions = monitorOptions.get("KEYWORD");
            for (DataSource dataSource : dataSources) {
                if (keywordOptions.containsKey(dataSource.getSource())) {
                    Map<String, Object> options = keywordOptions.get(dataSource.getSource());
                    Keyword keyword = Keyword.builder()
                            .monitorId(monitor.getId())
                            .dataSource(dataSource)
                            .keywords((String) options.getOrDefault("keywords", ""))
                            .spamKeywords((String) options.getOrDefault("spam_keywords", null))
                            .build();
                    monitor.getKeywords().add(keyword);
                }
            }
        }
        
        // Handle ACCOUNT_ANALYSIS monitor type
        if (monitorOptions.containsKey("ACCOUNT_ANALYSIS")) {
            Map<String, Map<String, Object>> accountOptions = monitorOptions.get("ACCOUNT_ANALYSIS");
            for (DataSource dataSource : dataSources) {
                if (accountOptions.containsKey(dataSource.getSource())) {
                    Map<String, Object> options = accountOptions.get(dataSource.getSource());
                    AccountAnalysis accountAnalysis = AccountAnalysis.builder()
                            .monitorId(monitor.getId())
                            .dataSource(dataSource)
                            .follow((String) options.getOrDefault("follow", ""))
                            .spamKeywords((String) options.getOrDefault("spam_keywords", null))
                            .excludedAccounts((String) options.getOrDefault("excluded_accounts", null))
                            .build();
                    monitor.getAccountAnalyses().add(accountAnalysis);
                }
            }
        }
        
        // Handle REGION monitor type
        if (monitorOptions.containsKey("REGION")) {
            Map<String, Map<String, Object>> regionOptions = monitorOptions.get("REGION");
            for (DataSource dataSource : dataSources) {
                if (regionOptions.containsKey(dataSource.getSource())) {
                    Map<String, Object> options = regionOptions.get(dataSource.getSource());
                    Regional regional = Regional.builder()
                            .monitorId(monitor.getId())
                            .dataSource(dataSource)
                            .mbr((String) options.getOrDefault("mbr", null))
                            .build();
                    monitor.getRegionals().add(regional);
                }
            }
        }

        // Handle MANAGED_ACCOUNT and MANAGED_PAGE §3.7, §6.2.4
        if (monitorOptions.containsKey("MANAGED_ACCOUNT")) {
            Map<String, Map<String, Object>> accOptions = monitorOptions.get("MANAGED_ACCOUNT");
            for (DataSource dataSource : dataSources) {
                if (accOptions.containsKey(dataSource.getSource())) {
                    Map<String, Object> options = accOptions.get(dataSource.getSource());
                    ManagedAccount ma = ManagedAccount.builder()
                            .monitorId(monitor.getId())
                            .dataSource(dataSource)
                            .accountName((String) options.getOrDefault("account_name", ""))
                            .accountId((String) options.getOrDefault("account_id", null))
                            .type("ACCOUNT")
                            .build();
                    if (monitor.getManagedAccounts() == null) monitor.setManagedAccounts(new java.util.ArrayList<>());
                    monitor.getManagedAccounts().add(ma);
                }
            }
        }
        if (monitorOptions.containsKey("MANAGED_PAGE")) {
            Map<String, Map<String, Object>> pageOptions = monitorOptions.get("MANAGED_PAGE");
            for (DataSource dataSource : dataSources) {
                if (pageOptions.containsKey(dataSource.getSource())) {
                    Map<String, Object> options = pageOptions.get(dataSource.getSource());
                    ManagedAccount ma = ManagedAccount.builder()
                            .monitorId(monitor.getId())
                            .dataSource(dataSource)
                            .accountName((String) options.getOrDefault("account_name", ""))
                            .accountId((String) options.getOrDefault("account_id", null))
                            .type("PAGE")
                            .build();
                    if (monitor.getManagedAccounts() == null) monitor.setManagedAccounts(new java.util.ArrayList<>());
                    monitor.getManagedAccounts().add(ma);
                }
            }
        }
    }
}
