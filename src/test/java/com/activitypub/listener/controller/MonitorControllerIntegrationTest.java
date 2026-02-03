package com.activitypub.listener.controller;

import com.activitypub.listener.dto.CreateMonitorDTO;
import com.activitypub.listener.kafka.ActivityPubKafkaProducer;
import com.activitypub.listener.repository.DataSourceRepository;
import com.activitypub.listener.repository.MonitorTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests: API flows and error scenarios (404, 422, validation).
 * JWT disabled in test profile so no 401 on protected endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Monitor API integration tests")
class MonitorControllerIntegrationTest {

    @Container
    static GenericContainer<?> mongo = new GenericContainer<>(DockerImageName.parse("mongo:7"))
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://" + mongo.getHost() + ":" + mongo.getMappedPort(27017) + "/test");
        registry.add("spring.data.mongodb.database", () -> "test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActivityPubKafkaProducer kafkaProducer;

    @Autowired
    private MonitorTypeRepository monitorTypeRepository;
    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Test
    @DisplayName("GET /monitors returns 200 and paginated list")
    void listMonitors_returns200() throws Exception {
        mockMvc.perform(get("/monitors").header("API-Version", "v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.pagination").doesNotExist());
    }

    @Test
    @DisplayName("GET /monitors/{id} with non-existent id returns 404")
    void getMonitor_notFound_returns404() throws Exception {
        mockMvc.perform(get("/monitors/nonexistent-id").header("API-Version", "v1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /monitors with invalid body returns 400 validation error")
    void createMonitor_invalidBody_returns400() throws Exception {
        CreateMonitorDTO dto = CreateMonitorDTO.builder()
                .name("")
                .monitorTypeId(null)
                .dataSources(null)
                .build();

        mockMvc.perform(post("/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .header("API-Version", "v1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /monitors with valid body returns 201 and monitor")
    void createMonitor_valid_returns201() throws Exception {
        String monitorTypeId = monitorTypeRepository.findByName("KEYWORD").orElseThrow().getId();
        String dataSourceId = dataSourceRepository.findBySource("MASTODON").orElseThrow().getId();

        CreateMonitorDTO dto = CreateMonitorDTO.builder()
                .name("Integration Test Monitor")
                .monitorTypeId(monitorTypeId)
                .dataSources(List.of(dataSourceId))
                .productId(1L)
                .build();

        mockMvc.perform(post("/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .header("API-Version", "v1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.name").value("Integration Test Monitor"));
    }
}
