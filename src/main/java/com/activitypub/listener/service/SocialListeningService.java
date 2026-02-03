package com.activitypub.listener.service;

import com.activitypub.listener.dto.SocialListeningRequestDTO;
import com.activitypub.listener.dto.SocialListeningResponseDTO;
import com.activitypub.listener.dto.WidgetDTO;
import com.activitypub.listener.exception.ResourceNotFoundException;
import com.activitypub.listener.kafka.ActivityPubKafkaProducer;
import com.activitypub.listener.kafka.SocialListeningAnalyticsMessage;
import com.activitypub.listener.model.DataSource;
import com.activitypub.listener.model.Metric;
import com.activitypub.listener.model.Monitor;
import com.activitypub.listener.model.SocialListeningJob;
import com.activitypub.listener.repository.DataSourceRepository;
import com.activitypub.listener.repository.MetricRepository;
import com.activitypub.listener.repository.MonitorRepository;
import com.activitypub.listener.repository.SocialListeningJobRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Social listening data request and widgets. §4.4.1, §4.4.2.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialListeningService {

    @Value("${social.listening.max-date-range-days:30}")
    private int maxDateRangeDays;

    private final MonitorRepository monitorRepository;
    private final DataSourceRepository dataSourceRepository;
    private final MetricRepository metricRepository;
    private final ParameterPreparationService parameterPreparationService;
    private final ActivityPubKafkaProducer kafkaProducer;
    private final SocialListeningJobRepository socialListeningJobRepository;

    /**
     * Validate request, prepare analytics message, send to Kafka, return topic/consumer group to client.
     */
    public SocialListeningResponseDTO requestData(SocialListeningRequestDTO request, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(request.getMonitorId())
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + request.getMonitorId()));

        if (userId != null && !monitor.getUserId().equals(userId)) {
            throw new IllegalStateException("Monitor does not belong to user");
        }
        if (monitor.getIsApproved() != Monitor.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Monitor must be approved to request social listening data");
        }
        if (Boolean.TRUE.equals(monitor.getPaused())) {
            throw new IllegalStateException("Monitor must not be paused");
        }

        DataSource dataSource = dataSourceRepository.findBySource(request.getDataSource())
                .orElseThrow(() -> new ResourceNotFoundException("Data source not found: " + request.getDataSource()));

        long start = request.getStartDate();
        long end = request.getEndDate();
        if (end < start) {
            throw new IllegalArgumentException("end_date must be >= start_date");
        }
        long rangeDays = (end - start) / (24 * 3600);
        if (rangeDays > maxDateRangeDays) {
            throw new IllegalArgumentException("Date range must not exceed " + maxDateRangeDays + " days");
        }

        if (request.getWidgetsNames() != null && !request.getWidgetsNames().isEmpty()) {
            List<Metric> validMetrics = metricRepository.findByActiveTrue();
            List<String> validNames = validMetrics.stream().map(Metric::getName).collect(Collectors.toList());
            for (String name : request.getWidgetsNames()) {
                if (!validNames.contains(name)) {
                    throw new IllegalArgumentException("Invalid widget name: " + name);
                }
            }
        }

        SocialListeningAnalyticsMessage message = parameterPreparationService.prepare(request, monitor);
        kafkaProducer.sendSocialListeningRequest(message);

        return SocialListeningResponseDTO.builder()
                .topic(message.getTopic())
                .consumerGroup(message.getConsumerGroup())
                .monitorId(monitor.getId())
                .manualTopicsEnabled(false)
                .monitorTopicsUsed(5)
                .build();
    }

    /**
     * Get available widgets/metrics for monitor and data source.
     */
    public List<WidgetDTO> getWidgets(String monitorId, String dataSource, String pageName) {
        monitorRepository.findByIdAndIsDeletedFalse(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + monitorId));
        dataSourceRepository.findBySource(dataSource)
                .orElseThrow(() -> new ResourceNotFoundException("Data source not found: " + dataSource));

        List<Metric> metrics = metricRepository.findByActiveTrue();
        return metrics.stream()
                .map(m -> WidgetDTO.builder()
                        .name(m.getName())
                        .displayName(m.getDisplayName() != null ? m.getDisplayName() : m.getName())
                        .chartType(m.getChartType())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Create async job for public API. §4.4.3
     */
    public SocialListeningJob createJob(SocialListeningRequestDTO request, Long userId) {
        Monitor monitor = monitorRepository.findByIdAndIsDeletedFalse(request.getMonitorId())
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + request.getMonitorId()));
        if (monitor.getIsApproved() != Monitor.ApprovalStatus.APPROVED || Boolean.TRUE.equals(monitor.getPaused())) {
            throw new IllegalStateException("Monitor must be approved and not paused");
        }
        dataSourceRepository.findBySource(request.getDataSource())
                .orElseThrow(() -> new ResourceNotFoundException("Data source not found: " + request.getDataSource()));

        SocialListeningJob job = SocialListeningJob.builder()
                .id(UUID.randomUUID().toString())
                .monitorId(request.getMonitorId())
                .userId(userId)
                .status("PENDING")
                .widgetsNames(request.getWidgetsNames())
                .build();
        job = socialListeningJobRepository.save(job);

        SocialListeningAnalyticsMessage message = parameterPreparationService.prepare(request, monitor);
        kafkaProducer.sendSocialListeningRequest(message);
        log.info("Created social listening job: {}", job.getId());
        return job;
    }

    /**
     * Get job status and optional widget data. §4.4.3
     */
    public Map<String, Object> getJobStatus(String jobId, String monitorId) {
        SocialListeningJob job = socialListeningJobRepository.findByIdAndMonitorId(jobId, monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        Map<String, Object> data = new HashMap<>();
        data.put("dataAvailable", "COMPLETED".equals(job.getStatus()) && job.getWidgetData() != null && !job.getWidgetData().isEmpty());
        data.put("status", job.getStatus());
        if (job.getWidgetData() != null) {
            data.put("widget_data", job.getWidgetData());
        }
        if (job.getNextJobId() != null) {
            data.put("next_job_id", job.getNextJobId());
        }
        return data;
    }
}
