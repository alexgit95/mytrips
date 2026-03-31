package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.ExportDto;
import com.alexgit95.MyTrips.model.AppUser;
import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.AppUserRepository;
import com.alexgit95.MyTrips.repository.CategoryRepository;
import com.alexgit95.MyTrips.repository.ExpenseRepository;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import com.alexgit95.MyTrips.repository.TripRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/mytrips-e2e-import-export.db",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=update"
})
class ImportExportEndToEndTest {

    // Supprime le fichier SQLite avant le chargement du contexte Spring (via ddl-auto=update)
    // afin de garantir un schéma frais à chaque fois.
    static {
        try {
            Files.deleteIfExists(Paths.get("./target/mytrips-e2e-import-export.db"));
        } catch (IOException ignored) {
        }
    }

    private static final String BACKUP_FILE = "/backup/mytrips-export-prod-20260330-064840.json";

    @Autowired
    private DataImportExportService dataImportExportService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private PlannerEventRepository plannerEventRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

        @Autowired
        private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDomainData() {
        plannerEventRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    void shouldImportModifyExportAndReimportWhilePreservingEntityCounts() throws Exception {
        ExportDto baseline;
        try (InputStream in = getClass().getResourceAsStream(BACKUP_FILE)) {
            assertNotNull(in, "Backup test file must exist");
            baseline = objectMapper.readValue(in, ExportDto.class);
        }

        try (InputStream in = getClass().getResourceAsStream(BACKUP_FILE)) {
            assertNotNull(in, "Backup test file must exist");
            dataImportExportService.importFromJson(in);
        }

        long baseTrips = baseline.getTrips() != null ? baseline.getTrips().size() : 0;
        long basePlannerEvents = baseline.getPlannerEvents() != null ? baseline.getPlannerEvents().size() : 0;
        long baseUsers = baseline.getUsers() != null ? baseline.getUsers().size() : 0;
        long baseExpenses = baseline.getExpenses() != null ? baseline.getExpenses().size() : 0;
        long baseCategories = baseline.getCategories() != null ? baseline.getCategories().size() : 0;

        assertEquals(baseTrips, tripRepository.count(), "Trip count after initial import");
        assertEquals(basePlannerEvents, plannerEventRepository.count(), "Planner event count after initial import");
        assertEquals(baseUsers, appUserRepository.count(), "User count after initial import");
        assertEquals(baseExpenses, expenseRepository.count(), "Expense count after initial import");
        assertEquals(baseCategories, categoryRepository.count(), "Category count after initial import");

        CategoryEntity addedCategory = categoryRepository.save(CategoryEntity.builder()
                .name("Categorie E2E Ajoutee")
                .icon("C")
                .color("#123456")
                .editable(true)
                .build());

        Trip addedTrip = tripRepository.save(Trip.builder()
                .name("Voyage E2E ajoute")
                .startDate(LocalDate.of(2030, 1, 10))
                .endDate(LocalDate.of(2030, 1, 12))
                .budget(BigDecimal.valueOf(999))
                .dailyBudgetThreshold(BigDecimal.valueOf(10))
                .dailyExpenseBudget(BigDecimal.valueOf(20))
                .build());

        plannerEventRepository.save(PlannerEvent.builder()
                .name("Evenement E2E ajoute")
                .eventDateTime(LocalDateTime.of(2030, 1, 10, 14, 0))
                .location("Lieu E2E")
                .comment("Commentaire E2E")
                .trip(addedTrip)
                .build());

        expenseRepository.save(Expense.builder()
                .amount(BigDecimal.valueOf(42))
                .date(LocalDate.of(2030, 1, 10))
                .category(addedCategory)
                .label("Depense E2E ajoutee")
                .numberOfDays(1)
                .trip(addedTrip)
                .build());

        appUserRepository.save(AppUser.builder()
                .username("e2e-user")
                .password("e2e-password-hash")
                .role("REPORTER")
                .build());

        long expectedTrips = baseTrips + 1;
        long expectedPlannerEvents = basePlannerEvents + 1;
        long expectedUsers = baseUsers + 1;
        long expectedExpenses = baseExpenses + 1;
        long expectedCategories = baseCategories + 1;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        dataImportExportService.exportToJson(out);

        ExportDto exported = objectMapper.readValue(out.toByteArray(), ExportDto.class);
        assertEquals(expectedTrips, exported.getTrips() != null ? exported.getTrips().size() : 0,
                "Trip count in exported backup");
        assertEquals(expectedPlannerEvents, exported.getPlannerEvents() != null ? exported.getPlannerEvents().size() : 0,
                "Planner event count in exported backup");
        assertEquals(expectedUsers, exported.getUsers() != null ? exported.getUsers().size() : 0,
                "User count in exported backup");
        assertEquals(expectedExpenses, exported.getExpenses() != null ? exported.getExpenses().size() : 0,
                "Expense count in exported backup");
        assertEquals(expectedCategories, exported.getCategories() != null ? exported.getCategories().size() : 0,
                "Category count in exported backup");

        dataImportExportService.importFromJson(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(expectedTrips, tripRepository.count(), "Trip count after re-import");
        assertEquals(expectedPlannerEvents, plannerEventRepository.count(), "Planner event count after re-import");
        assertEquals(expectedUsers, appUserRepository.count(), "User count after re-import");
        assertEquals(expectedExpenses, expenseRepository.count(), "Expense count after re-import");
        assertEquals(expectedCategories, categoryRepository.count(), "Category count after re-import");
    }
}
