package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.model.ApiAccessKey;
import com.alexgit95.MyTrips.repository.ApiAccessKeyRepository;
import com.alexgit95.MyTrips.service.ApiAccessKeyService;
import com.alexgit95.MyTrips.service.DataImportExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/mytrips-api-keys-web-test.db",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create"
})
class ApiExportApiKeySecurityTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private ApiAccessKeyService apiAccessKeyService;

    @Autowired
    private ApiAccessKeyRepository apiAccessKeyRepository;

    @MockitoBean
    private DataImportExportService dataImportExportService;

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        apiAccessKeyRepository.deleteAll();
        doAnswer(invocation -> {
            invocation.<java.io.OutputStream>getArgument(0).write("{}".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(dataImportExportService).exportToJson(any());
    }

    @Test
    void exportApi_shouldReturnUnauthorizedWhenMissingApiKey() throws Exception {
        mockMvc.perform(get("/api/admin/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportApi_shouldReturnUnauthorizedWhenApiKeyIsInvalid() throws Exception {
        mockMvc.perform(get("/api/admin/export").queryParam("apiKey", "invalid-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportApi_shouldReturnUnauthorizedWhenApiKeyIsExpired() throws Exception {
        ApiAccessKeyService.GeneratedApiKey generated = apiAccessKeyService.generateKey(Duration.ofDays(1), null);
        ApiAccessKey persisted = apiAccessKeyRepository.findTopByOrderByCreatedAtDesc().orElseThrow();
        persisted.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        apiAccessKeyRepository.save(persisted);

        mockMvc.perform(get("/api/admin/export").queryParam("apiKey", generated.rawKey()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportApi_shouldReturnJsonWhenApiKeyIsValid() throws Exception {
        ApiAccessKeyService.GeneratedApiKey generated = apiAccessKeyService.generateKey(Duration.ofDays(7), null);

        mockMvc.perform(get("/api/admin/export").queryParam("apiKey", generated.rawKey()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(content().json("{}"));

        verify(dataImportExportService).exportToJson(any());
    }

    @Test
    void exportApi_shouldReturnUnauthorizedAfterCompleteRevocation() throws Exception {
        ApiAccessKeyService.GeneratedApiKey generated = apiAccessKeyService.generateKey(Duration.ofDays(30), null);
        apiAccessKeyService.revokeAllActiveKeys();

        mockMvc.perform(get("/api/admin/export").queryParam("apiKey", generated.rawKey()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportApi_shouldIgnoreHeaderApiKeyWhenQueryParamMissing() throws Exception {
        ApiAccessKeyService.GeneratedApiKey generated = apiAccessKeyService.generateKey(Duration.ofDays(30), null);

        mockMvc.perform(get("/api/admin/export").header("X-API-Key", generated.rawKey()))
                .andExpect(status().isUnauthorized());
    }
}
