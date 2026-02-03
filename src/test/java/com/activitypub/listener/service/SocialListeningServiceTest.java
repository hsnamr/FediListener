package com.activitypub.listener.service;

import com.activitypub.listener.dto.SocialListeningRequestDTO;
import com.activitypub.listener.dto.SocialListeningResponseDTO;
import com.activitypub.listener.exception.ResourceNotFoundException;
import com.activitypub.listener.kafka.ActivityPubKafkaProducer;
import com.activitypub.listener.kafka.SocialListeningAnalyticsMessage;
import com.activitypub.listener.model.DataSource;
import com.activitypub.listener.model.Metric;
import com.activitypub.listener.model.Monitor;
import com.activitypub.listener.model.MonitorType;
import com.activitypub.listener.repository.DataSourceRepository;
import com.activitypub.listener.repository.MetricRepository;
import com.activitypub.listener.repository.MonitorRepository;
import com.activitypub.listener.repository.SocialListeningJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("SocialListeningService unit tests")
class SocialListeningServiceTest {

    @MockBean
    private MonitorRepository monitorRepository;
    @MockBean
    private DataSourceRepository dataSourceRepository;
    @MockBean
    private MetricRepository metricRepository;
    @MockBean
    private ParameterPreparationService parameterPreparationService;
    @MockBean
    private ActivityPubKafkaProducer kafkaProducer;
    @MockBean
    private SocialListeningJobRepository socialListeningJobRepository;

    @Autowired
    private SocialListeningService socialListeningService;

    private Monitor monitor;
    private DataSource dataSource;
    private SocialListeningRequestDTO request;

    @BeforeEach
    void setUp() {
        MonitorType mt = MonitorType.builder().id("mt1").name("KEYWORD").build();
        dataSource = DataSource.builder().id("ds1").source("MASTODON").build();
        monitor = Monitor.builder()
                .id("mon1")
                .name("Monitor")
                .monitorType(mt)
                .userId(100L)
                .isApproved(Monitor.ApprovalStatus.APPROVED)
                .paused(false)
                .build();
        long start = System.currentTimeMillis() / 1000;
        long end = start + 86400; // 1 day
        request = SocialListeningRequestDTO.builder()
                .monitorId("mon1")
                .dataSource("MASTODON")
                .pageName("account_page")
                .startDate(start)
                .endDate(end)
                .widgetsNames(List.of("posts_count"))
                .build();
    }

    @Test
    @DisplayName("requestData throws when monitor not found")
    void requestData_monitorNotFound_throws() {
        when(monitorRepository.findByIdAndIsDeletedFalse("mon1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> socialListeningService.requestData(request, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Monitor not found");
        verify(kafkaProducer, never()).sendSocialListeningRequest(any());
    }

    @Test
    @DisplayName("requestData throws when user does not own monitor")
    void requestData_wrongUser_throws() {
        when(monitorRepository.findByIdAndIsDeletedFalse("mon1")).thenReturn(Optional.of(monitor));

        assertThatThrownBy(() -> socialListeningService.requestData(request, 999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to user");
        verify(kafkaProducer, never()).sendSocialListeningRequest(any());
    }

    @Test
    @DisplayName("requestData throws when monitor not approved")
    void requestData_monitorNotApproved_throws() {
        monitor.setIsApproved(Monitor.ApprovalStatus.UNAPPROVED);
        when(monitorRepository.findByIdAndIsDeletedFalse("mon1")).thenReturn(Optional.of(monitor));

        assertThatThrownBy(() -> socialListeningService.requestData(request, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be approved");
        verify(kafkaProducer, never()).sendSocialListeningRequest(any());
    }

    @Test
    @DisplayName("requestData throws when date range exceeds max")
    void requestData_dateRangeTooLarge_throws() {
        when(monitorRepository.findByIdAndIsDeletedFalse("mon1")).thenReturn(Optional.of(monitor));
        when(dataSourceRepository.findBySource("MASTODON")).thenReturn(Optional.of(dataSource));
        long start = request.getStartDate();
        request.setEndDate(start + 60 * 60 * 24 * 31); // 31 days

        assertThatThrownBy(() -> socialListeningService.requestData(request, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date range");
        verify(kafkaProducer, never()).sendSocialListeningRequest(any());
    }

    @Test
    @DisplayName("requestData sends Kafka message and returns response")
    void requestData_valid_sendsKafkaAndReturnsResponse() {
        SocialListeningAnalyticsMessage message = SocialListeningAnalyticsMessage.builder()
                .topic("account_page-data-monitor-mon1")
                .consumerGroup("frontend-consumer-account_page-monitor-mon1")
                .monitorId("mon1")
                .build();
        when(monitorRepository.findByIdAndIsDeletedFalse("mon1")).thenReturn(Optional.of(monitor));
        when(dataSourceRepository.findBySource("MASTODON")).thenReturn(Optional.of(dataSource));
        when(metricRepository.findByActiveTrue()).thenReturn(
                List.of(Metric.builder().name("posts_count").displayName("Posts").chartType("bar").build()));
        when(parameterPreparationService.prepare(request, monitor)).thenReturn(message);

        SocialListeningResponseDTO result = socialListeningService.requestData(request, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getMonitorId()).isEqualTo("mon1");
        assertThat(result.getTopic()).isEqualTo("account_page-data-monitor-mon1");
        assertThat(result.getConsumerGroup()).isEqualTo("frontend-consumer-account_page-monitor-mon1");

        ArgumentCaptor<SocialListeningAnalyticsMessage> captor = ArgumentCaptor.forClass(SocialListeningAnalyticsMessage.class);
        verify(kafkaProducer).sendSocialListeningRequest(captor.getValue());
        assertThat(captor.getValue().getMonitorId()).isEqualTo("mon1");
    }
}
