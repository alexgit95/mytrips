package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.ExportDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BackupFormatCompatibilityTest {

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Test
    void shouldReadGoldenBackupFormatV1() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/backup/export-format-v1.json")) {
            assertNotNull(in, "Golden file export-format-v1.json must exist");

            ExportDto dto = objectMapper().readValue(in, ExportDto.class);

            assertNotNull(dto.getCategories());
            assertNotNull(dto.getTrips());
            assertNotNull(dto.getExpenses());
            assertNotNull(dto.getPlannerEvents());
            assertNotNull(dto.getUsers());
            assertEquals(1, dto.getTrips().size());
            assertEquals("Trip A", dto.getTrips().get(0).getName());
            assertEquals("Transport", dto.getCategories().get(0).getName());
            assertEquals("ADMIN", dto.getUsers().get(0).getRole());
        }
    }

    @Test
    void shouldReadBackupWithMissingSectionsAndUnknownFields() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/backup/export-format-with-missing-sections.json")) {
            assertNotNull(in, "Compatibility file export-format-with-missing-sections.json must exist");

            ExportDto dto = objectMapper().readValue(in, ExportDto.class);

            assertNotNull(dto);
            assertNotNull(dto.getTrips());
            assertEquals(1, dto.getTrips().size());
            assertNull(dto.getCategories());
            assertNull(dto.getExpenses());
            assertNull(dto.getPlannerEvents());
            assertNull(dto.getUsers());
        }
    }

    @Test
    void shouldKeepStableTopLevelKeysOnSerialization() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(java.util.List.of())
                .trips(java.util.List.of())
                .expenses(java.util.List.of())
                .plannerEvents(java.util.List.of())
                .users(java.util.List.of())
                .build();

        String json = objectMapper().writeValueAsString(dto);
        JsonNode root = objectMapper().readTree(json);

        Set<String> expected = Set.of("categories", "trips", "expenses", "plannerEvents", "users");
        expected.forEach(key -> assertTrue(root.has(key), "Missing top-level key: " + key));
    }
}
