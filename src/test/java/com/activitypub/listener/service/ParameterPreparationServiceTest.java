package com.activitypub.listener.service;

import com.activitypub.listener.dto.SocialListeningFiltersDTO;
import com.activitypub.listener.dto.SocialListeningRequestDTO;
import com.activitypub.listener.kafka.SocialListeningAnalyticsMessage;
import com.activitypub.listener.model.DataSource;
import com.activitypub.listener.model.Monitor;
import com.activitypub.listener.model.MonitorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("ParameterPreparationService unit tests")
class ParameterPreparationServiceTest {

    @Autowired
    private ParameterPreparationService parameterPreparationService;

    private Monitor monitor;
    private SocialListeningRequestDTO request;

    @BeforeEach
    void setUp() {
        MonitorType mt = MonitorType.builder().id("mt1").name("KEYWORD").build();
        DataSource ds = DataSource.builder().id("ds1").source("MASTODON").build();
        monitor = Monitor.builder()
                .id("mon1")
                .name("Monitor")
                .monitorType(mt)
                .userId(100L)
                .build();
        long now = System.currentTimeMillis() / 1000;
        request = SocialListeningRequestDTO.builder()
                .monitorId("mon1")
                .dataSource("MASTODON")
                .pageName("account_page")
                .startDate(now)
                .endDate(now + 86400)
                .widgetsNames(List.of("posts_count"))
                .pageNumber(2)
                .filters(SocialListeningFiltersDTO.builder().build())
                .build();
    }

    @Test
    @DisplayName("prepare builds topic and consumer group from pageName and monitorId")
    void prepare_buildsTopicAndConsumerGroup() {
        SocialListeningAnalyticsMessage msg = parameterPreparationService.prepare(request, monitor);

        assertThat(msg).isNotNull();
        assertThat(msg.getTopic()).isEqualTo("account_page-data-monitor-mon1");
        assertThat(msg.getConsumerGroup()).startsWith("frontend-consumer-").contains("account_page").contains("mon1");
        assertThat(msg.getMonitorId()).isEqualTo("mon1");
        assertThat(msg.getPageName()).isEqualTo("account_page");
        assertThat(msg.getPageNumber()).isEqualTo(2);
        assertThat(msg.getMonitorType()).isEqualTo("KEYWORD");
        assertThat(msg.getUserId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("prepare uses default pageNumber when null")
    void prepare_defaultPageNumber() {
        request.setPageNumber(null);
        SocialListeningAnalyticsMessage msg = parameterPreparationService.prepare(request, monitor);

        assertThat(msg.getPageNumber()).isEqualTo(1);
    }
}
