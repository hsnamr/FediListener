package com.activitypub.listener.service;

import com.activitypub.listener.dto.CreateMonitorDTO;
import com.activitypub.listener.dto.MonitorDTO;
import com.activitypub.listener.dto.PaginationResponse;
import com.activitypub.listener.dto.UpdateMonitorDTO;
import com.activitypub.listener.exception.ResourceNotFoundException;
import com.activitypub.listener.kafka.ActivityPubKafkaProducer;
import com.activitypub.listener.mapper.MonitorMapper;
import com.activitypub.listener.model.*;
import com.activitypub.listener.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitorService unit tests")
class MonitorServiceTest {

    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private MonitorTypeRepository monitorTypeRepository;
    @Mock
    private DataSourceRepository dataSourceRepository;
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private MonitorMapper monitorMapper;
    @Mock
    private ActivityPubKafkaProducer kafkaProducer;
    @Mock
    private com.activitypub.listener.repository.CollectedActivityRepository collectedActivityRepository;

    @InjectMocks
    private MonitorService monitorService;

    private MonitorType monitorType;
    private DataSource dataSource;
    private Monitor monitor;
    private MonitorDTO monitorDTO;
    private CreateMonitorDTO createDto;

    @BeforeEach
    void setUp() {
        monitorType = MonitorType.builder().id("mt1").name("KEYWORD").build();
        dataSource = DataSource.builder().id("ds1").source("MASTODON").build();
        monitor = Monitor.builder()
                .id("m1")
                .name("Test Monitor")
                .monitorType(monitorType)
                .userId(100L)
                .creatorId(100L)
                .isApproved(Monitor.ApprovalStatus.APPROVED)
                .paused(false)
                .isDeleted(false)
                .keywords(new java.util.ArrayList<>())
                .accountAnalyses(new java.util.ArrayList<>())
                .regionals(new java.util.ArrayList<>())
                .build();
        monitorDTO = MonitorDTO.builder().id("m1").name("Test Monitor").build();
        createDto = CreateMonitorDTO.builder()
                .name("New Monitor")
                .monitorTypeId("mt1")
                .dataSources(List.of("ds1"))
                .productId(1L)
                .build();
    }

    @Test
    @DisplayName("createMonitor throws when monitor type not found")
    void createMonitor_monitorTypeNotFound_throws() {
        when(monitorTypeRepository.findById("mt1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> monitorService.createMonitor(createDto, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Monitor type not found");
        verify(monitorRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMonitor throws when data source not found")
    void createMonitor_dataSourceNotFound_throws() {
        when(monitorTypeRepository.findById("mt1")).thenReturn(Optional.of(monitorType));
        when(dataSourceRepository.findAllById(List.of("ds1"))).thenReturn(List.of()); // missing ds1

        assertThatThrownBy(() -> monitorService.createMonitor(createDto, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("data sources not found");
        verify(monitorRepository, never()).save(any());
    }

    @Test
    @DisplayName("createMonitor throws when user exceeds monitor quota")
    void createMonitor_quotaExceeded_throws() {
        when(monitorTypeRepository.findById("mt1")).thenReturn(Optional.of(monitorType));
        when(dataSourceRepository.findAllById(List.of("ds1"))).thenReturn(List.of(dataSource));
        when(monitorRepository.countByUserIdAndIsDeletedFalse(100L)).thenReturn(100L);

        assertThatThrownBy(() -> monitorService.createMonitor(createDto, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Monitor limit exceeded");
        verify(monitorRepository, never()).save(any());
    }

    @Test
    @DisplayName("getMonitor throws when monitor not found")
    void getMonitor_notFound_throws() {
        when(monitorRepository.findByIdAndIsDeletedFalse("m1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> monitorService.getMonitor("m1", 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Monitor not found");
    }

    @Test
    @DisplayName("getMonitor throws when user does not own monitor")
    void getMonitor_wrongUser_throws() {
        when(monitorRepository.findByIdAndIsDeletedFalse("m1")).thenReturn(Optional.of(monitor));

        assertThatThrownBy(() -> monitorService.getMonitor("m1", 999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not belong to user");
    }

    @Test
    @DisplayName("getMonitor returns DTO when user owns monitor")
    void getMonitor_owner_returnsDto() {
        when(monitorRepository.findByIdAndIsDeletedFalse("m1")).thenReturn(Optional.of(monitor));
        when(monitorMapper.toDTO(monitor)).thenReturn(monitorDTO);

        MonitorDTO result = monitorService.getMonitor("m1", 100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("m1");
        assertThat(result.getName()).isEqualTo("Test Monitor");
    }

    @Test
    @DisplayName("listMonitors returns paginated response")
    void listMonitors_returnsPaginated() {
        Page<Monitor> page = new PageImpl<>(List.of(monitor), Pageable.ofSize(10), 1);
        when(monitorRepository.findMonitors(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);
        when(monitorMapper.toDTO(monitor)).thenReturn(monitorDTO);

        PaginationResponse<MonitorDTO> result = monitorService.listMonitors(
                Pageable.ofSize(10), null, null, null, null, null, null, 100L, null, "asc");

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getId()).isEqualTo("m1");
        assertThat(result.getPagination().getTotal()).isEqualTo(1);
    }
}
