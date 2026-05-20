package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.service.OfflineSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/mytrips-offline-sync-test.db",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create"
})
class OfflineSyncControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private OfflineSyncService offlineSyncService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void sync_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/offline/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actions\":[]}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    void sync_shouldReturnForbiddenForGuestRole() throws Exception {
        mockMvc.perform(post("/api/offline/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actions\":[]}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sync_shouldReturnOkWithEmptyActions() throws Exception {
        mockMvc.perform(post("/api/offline/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actions\":[]}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(0))
                .andExpect(jsonPath("$.failed").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sync_shouldProcessActionsSuccessfully() throws Exception {
        doNothing().when(offlineSyncService).processAction(any());

        String body = """
                {
                    "actions": [
                        {
                            "type": "expense",
                            "tripId": 1,
                            "timestamp": "2026-05-19T12:00:00Z",
                            "data": {
                                "label": "Test",
                                "amount": 10.0,
                                "date": "2026-05-19",
                                "categoryId": "1",
                                "numberOfDays": 1,
                                "isPaid": false
                            }
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/offline/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(1))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.message").value("Synchronisation réussie"));

        verify(offlineSyncService, times(1)).processAction(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sync_shouldReportFailedActions() throws Exception {
        doThrow(new RuntimeException("DB error")).when(offlineSyncService).processAction(any());

        String body = """
                {
                    "actions": [
                        {
                            "type": "expense",
                            "tripId": 1,
                            "timestamp": "2026-05-19T12:00:00Z",
                            "data": {
                                "label": "Test",
                                "amount": 10.0,
                                "date": "2026-05-19",
                                "categoryId": "1",
                                "numberOfDays": 1,
                                "isPaid": false
                            }
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/offline/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(0))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.message").value("Synchronisation partielle"));
    }

    @Test
    @WithMockUser(roles = "REPORTER")
    void sync_shouldAllowReporterRole() throws Exception {
        doNothing().when(offlineSyncService).processAction(any());

        String body = """
                {
                    "actions": [
                        {
                            "type": "planner_event",
                            "tripId": 1,
                            "timestamp": "2026-05-19T12:00:00Z",
                            "data": {
                                "name": "Event",
                                "eventDateTime": "2026-05-19T14:00:00",
                                "location": "",
                                "latitude": null,
                                "longitude": null,
                                "comment": ""
                            }
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/offline/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(1));
    }
}
