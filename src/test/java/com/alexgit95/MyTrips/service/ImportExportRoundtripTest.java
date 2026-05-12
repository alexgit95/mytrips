package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.ExportDto;
import com.alexgit95.MyTrips.dto.ExpenseExportDto;
import com.alexgit95.MyTrips.dto.PlannerEventExportDto;
import com.alexgit95.MyTrips.dto.TripExportDto;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.AccommodationRepository;
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
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E tests verifying full data fidelity through import → export → re-import cycles.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/mytrips-e2e-roundtrip.db",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=update"
})
class ImportExportRoundtripTest {

    static {
        try {
            Files.deleteIfExists(Paths.get("./target/mytrips-e2e-roundtrip.db"));
        } catch (IOException ignored) {
        }
    }

    private static final String ROUNDTRIP_FILE = "/backup/export-roundtrip-test.json";

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
    private AccommodationRepository accommodationRepository;

    @Autowired
    private AppSettingsService appSettingsService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDomainData() {
        accommodationRepository.deleteAllInBatch();
        plannerEventRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
        tripRepository.deleteAllInBatch();
        appUserRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    void importAndExport_shouldPreserveAllTripFields() throws Exception {
        importTestFile();

        List<Trip> trips = tripRepository.findAllByOrderByStartDateAsc();
        assertEquals(2, trips.size());

        Trip italie = trips.stream().filter(t -> t.getName().equals("Voyage Italie")).findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(1500).setScale(2), italie.getBudget().setScale(2));
        assertEquals(41.9028, italie.getLatitude(), 0.001);
        assertEquals(12.4964, italie.getLongitude(), 0.001);
        assertEquals(BigDecimal.valueOf(50).setScale(2), italie.getDailyBudgetThreshold().setScale(2));
        assertEquals(BigDecimal.valueOf(80).setScale(2), italie.getDailyExpenseBudget().setScale(2));

        Trip paris = trips.stream().filter(t -> t.getName().equals("Week-end Paris")).findFirst().orElseThrow();
        assertNull(paris.getLatitude());
        assertNull(paris.getLongitude());
    }

    @Test
    void importAndExport_shouldPreserveExpenseIsPaidAndNumberOfDays() throws Exception {
        importTestFile();

        List<Expense> expenses = expenseRepository.findAll();
        assertEquals(4, expenses.size());

        Expense vol = expenses.stream().filter(e -> e.getLabel().equals("Vol Paris-Rome")).findFirst().orElseThrow();
        assertTrue(vol.getIsPaid());
        assertEquals(1, vol.getNumberOfDays());

        Expense location = expenses.stream().filter(e -> e.getLabel().equals("Location voiture")).findFirst().orElseThrow();
        assertTrue(location.getIsPaid());
        assertEquals(5, location.getNumberOfDays());
        assertEquals(BigDecimal.valueOf(210).setScale(2), location.getAmount().setScale(2));

        Expense restaurant = expenses.stream().filter(e -> e.getLabel().equals("Restaurant Trastevere")).findFirst().orElseThrow();
        assertFalse(restaurant.getIsPaid());
    }

    @Test
    void importAndExport_shouldPreservePlannerEventGpsCoordinates() throws Exception {
        importTestFile();

        List<PlannerEvent> events = plannerEventRepository.findAll();
        assertEquals(3, events.size());

        PlannerEvent fiumicino = events.stream().filter(e -> e.getName().equals("Arrivee Rome")).findFirst().orElseThrow();
        assertEquals(41.8003, fiumicino.getLatitude(), 0.001);
        assertEquals(12.2389, fiumicino.getLongitude(), 0.001);
        assertEquals("Terminal 3", fiumicino.getComment());

        PlannerEvent colisee = events.stream().filter(e -> e.getName().equals("Colisee")).findFirst().orElseThrow();
        assertEquals(41.8902, colisee.getLatitude(), 0.001);
        assertNull(colisee.getComment());
    }

    @Test
    void importAndExport_shouldPreserveHomeCountry() throws Exception {
        importTestFile();

        assertEquals("FR", appSettingsService.getHomeCountry());
    }

    @Test
    void importAndExport_shouldPreserveUserPasswordHashes() throws Exception {
        importTestFile();

        assertEquals(2, appUserRepository.count());
        var admin = appUserRepository.findByUsername("admin").orElseThrow();
        assertTrue(admin.getPassword().startsWith("$2a$10$"));
        assertEquals("ADMIN", admin.getRole());

        var reporter = appUserRepository.findByUsername("reporter").orElseThrow();
        assertEquals("REPORTER", reporter.getRole());
    }

    @Test
    void fullRoundtrip_exportedDataShouldMatchImportedData() throws Exception {
        // Import original
        importTestFile();

        // Export
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        dataImportExportService.exportToJson(out);
        ExportDto exported = objectMapper.readValue(out.toByteArray(), ExportDto.class);

        // Verify export structure matches
        assertEquals(2, exported.getCategories().size());
        assertEquals(2, exported.getTrips().size());
        assertEquals(4, exported.getExpenses().size());
        assertEquals(3, exported.getPlannerEvents().size());
        assertEquals(2, exported.getUsers().size());

        // Verify trip data fidelity in export
        TripExportDto italieExported = exported.getTrips().stream()
                .filter(t -> t.getName().equals("Voyage Italie")).findFirst().orElseThrow();
        assertEquals(41.9028, italieExported.getLatitude(), 0.001);
        assertEquals(12.4964, italieExported.getLongitude(), 0.001);

        // Verify expense data fidelity in export
        ExpenseExportDto volExported = exported.getExpenses().stream()
                .filter(e -> e.getLabel().equals("Vol Paris-Rome")).findFirst().orElseThrow();
        assertTrue(volExported.getIsPaid());
        assertEquals(BigDecimal.valueOf(120.50).setScale(2), volExported.getAmount().setScale(2));

        ExpenseExportDto locationExported = exported.getExpenses().stream()
                .filter(e -> e.getLabel().equals("Location voiture")).findFirst().orElseThrow();
        assertEquals(5, locationExported.getNumberOfDays());

        // Verify planner event data fidelity
        PlannerEventExportDto coliseeExported = exported.getPlannerEvents().stream()
                .filter(pe -> pe.getName().equals("Colisee")).findFirst().orElseThrow();
        assertEquals(41.8902, coliseeExported.getLatitude(), 0.001);
        assertEquals(12.4922, coliseeExported.getLongitude(), 0.001);

        // Re-import from exported data
        cleanDomainData();
        dataImportExportService.importFromJson(new ByteArrayInputStream(out.toByteArray()));

        // Counts should be identical
        assertEquals(2, tripRepository.count());
        assertEquals(4, expenseRepository.count());
        assertEquals(3, plannerEventRepository.count());
        assertEquals(2, appUserRepository.count());
    }

    @Test
    void importTwice_shouldBeIdempotent() throws Exception {
        importTestFile();

        long tripsAfterFirst = tripRepository.count();
        long expensesAfterFirst = expenseRepository.count();
        long eventsAfterFirst = plannerEventRepository.count();

        // Import same file again (full replace)
        importTestFile();

        assertEquals(tripsAfterFirst, tripRepository.count());
        assertEquals(expensesAfterFirst, expenseRepository.count());
        assertEquals(eventsAfterFirst, plannerEventRepository.count());
    }

    @Test
    void importFromJson_shouldHandleLegacyFormatWithoutHomeCountry() throws Exception {
        // File that has no homeCountry field
        try (InputStream in = getClass().getResourceAsStream("/backup/export-format-with-missing-sections.json")) {
            assertNotNull(in);
            dataImportExportService.importFromJson(in);
        }
        // Should not crash and homeCountry should remain whatever it was before
        assertNotNull(appSettingsService.getHomeCountry());
    }

    @Test
    void importFromJson_shouldAssociateExpensesWithCorrectTrips() throws Exception {
        importTestFile();

        List<Trip> trips = tripRepository.findAllByOrderByStartDateAsc();
        Trip italie = trips.stream().filter(t -> t.getName().equals("Voyage Italie")).findFirst().orElseThrow();
        Trip paris = trips.stream().filter(t -> t.getName().equals("Week-end Paris")).findFirst().orElseThrow();

        List<Expense> italieExpenses = expenseRepository.findByTripIdOrderByDateAsc(italie.getId());
        List<Expense> parisExpenses = expenseRepository.findByTripIdOrderByDateAsc(paris.getId());

        assertEquals(3, italieExpenses.size(), "Voyage Italie should have 3 expenses");
        assertEquals(1, parisExpenses.size(), "Week-end Paris should have 1 expense");
        assertEquals("TGV A/R", parisExpenses.get(0).getLabel());
    }

    @Test
    void importFromJson_shouldAssociatePlannerEventsWithCorrectTrips() throws Exception {
        importTestFile();

        List<Trip> trips = tripRepository.findAllByOrderByStartDateAsc();
        Trip italie = trips.stream().filter(t -> t.getName().equals("Voyage Italie")).findFirst().orElseThrow();
        Trip paris = trips.stream().filter(t -> t.getName().equals("Week-end Paris")).findFirst().orElseThrow();

        List<PlannerEvent> italieEvents = plannerEventRepository.findByTripIdOrderByEventDateTimeAsc(italie.getId());
        List<PlannerEvent> parisEvents = plannerEventRepository.findByTripIdOrderByEventDateTimeAsc(paris.getId());

        assertEquals(2, italieEvents.size(), "Voyage Italie should have 2 planner events");
        assertEquals(1, parisEvents.size(), "Week-end Paris should have 1 planner event");
        assertEquals("Tour Eiffel", parisEvents.get(0).getName());
    }

    private void importTestFile() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(ROUNDTRIP_FILE)) {
            assertNotNull(in, "Test file must exist: " + ROUNDTRIP_FILE);
            dataImportExportService.importFromJson(in);
        }
    }
}
