package com.activitypub.listener.config;

import com.activitypub.listener.model.DataSource;
import com.activitypub.listener.model.MonitorType;
import com.activitypub.listener.model.Metric;
import com.activitypub.listener.repository.DataSourceRepository;
import com.activitypub.listener.repository.MetricRepository;
import com.activitypub.listener.repository.MonitorTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoDataInitializer implements CommandLineRunner {
    
    private final MonitorTypeRepository monitorTypeRepository;
    private final DataSourceRepository dataSourceRepository;
    private final MetricRepository metricRepository;
    
    @Override
    public void run(String... args) {
        initializeMonitorTypes();
        initializeDataSources();
        initializeMetrics();
    }
    
    private void initializeMonitorTypes() {
        if (monitorTypeRepository.count() == 0) {
            log.info("Initializing monitor types...");
            List<MonitorType> monitorTypes = Arrays.asList(
                MonitorType.builder().name("KEYWORD").description("Keyword-based monitoring").build(),
                MonitorType.builder().name("ACCOUNT_ANALYSIS").description("Account/profile monitoring").build(),
                MonitorType.builder().name("REGION").description("Regional/geographic monitoring").build(),
                MonitorType.builder().name("MANAGED_ACCOUNT").description("Managed social media account").build(),
                MonitorType.builder().name("MANAGED_PAGE").description("Managed social media page").build()
            );
            monitorTypeRepository.saveAll(monitorTypes);
            log.info("Monitor types initialized: {}", monitorTypes.size());
        }
    }
    
    private void initializeDataSources() {
        if (dataSourceRepository.count() == 0) {
            log.info("Initializing data sources...");
            List<DataSource> dataSources = Arrays.asList(
                DataSource.builder().source("MASTODON").category("social media").instanceType("mastodon").build(),
                DataSource.builder().source("LEMMY").category("social media").instanceType("lemmy").build(),
                DataSource.builder().source("PEERTUBE").category("social media").instanceType("peertube").build(),
                DataSource.builder().source("PLEROMA").category("social media").instanceType("pleroma").build(),
                DataSource.builder().source("MISSKEY").category("social media").instanceType("misskey").build()
            );
            dataSourceRepository.saveAll(dataSources);
            log.info("Data sources initialized: {}", dataSources.size());
        }
    }
    
    private void initializeMetrics() {
        if (metricRepository.count() == 0) {
            log.info("Initializing metrics...");
            List<Metric> metrics = Arrays.asList(
                Metric.builder().name("engagements").displayName("Engagements").chartType("numberChartWidget").build(),
                Metric.builder().name("posts").displayName("Posts").chartType("tableWidget").build(),
                Metric.builder().name("authors").displayName("Authors").chartType("tableWidget").build(),
                Metric.builder().name("topics").displayName("Topics").chartType("chartWidget").build(),
                Metric.builder().name("sentiment").displayName("Sentiment").chartType("chartWidget").build(),
                Metric.builder().name("top_cities").displayName("Top Cities").chartType("chartWidget").build(),
                Metric.builder().name("top_countries").displayName("Top Countries").chartType("chartWidget").build(),
                Metric.builder().name("gender_distribution").displayName("Gender Distribution").chartType("chartWidget").build(),
                Metric.builder().name("languages").displayName("Languages").chartType("chartWidget").build()
            );
            metricRepository.saveAll(metrics);
            log.info("Metrics initialized: {}", metrics.size());
        }
    }
}
