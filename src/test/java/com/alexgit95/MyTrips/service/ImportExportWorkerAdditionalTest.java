package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.AccommodationExportDto;
import com.alexgit95.MyTrips.dto.CategoryExportDto;
import com.alexgit95.MyTrips.dto.ExpenseExportDto;
import com.alexgit95.MyTrips.dto.ExportDto;
import com.alexgit95.MyTrips.dto.PlannerEventExportDto;
import com.alexgit95.MyTrips.dto.TripExportDto;
import com.alexgit95.MyTrips.dto.UserExportDto;
import com.alexgit95.MyTrips.model.AppUser;
import com.alexgit95.MyTrips.model.CategoryEntity;
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
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportExportWorkerAdditionalTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private PlannerEventRepository plannerEventRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private AccommodationRepository accommodationRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AppSettingsService appSettingsService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ImportExportWorker worker;

    // ========================================================================
    // EXPORT TESTS
    // ========================================================================

    @Test
    void exportToJson_shouldIncludeHomeCountrySetting() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(tripRepository.findAll()).thenReturn(List.of());
        when(expenseRepository.findAll()).thenReturn(List.of());
        when(plannerEventRepository.findAll()).thenReturn(List.of());
        when(appUserRepository.findAll()).thenReturn(List.of());
        when(accommodationRepository.findAll()).thenReturn(List.of());
        when(appSettingsService.getHomeCountry()).thenReturn("DE");

        ObjectWriter writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        worker.exportToJson(out);

        ArgumentCaptor<ExportDto> captor = ArgumentCaptor.forClass(ExportDto.class);
        verify(writer).writeValue(eq(out), captor.capture());

        assertEquals("DE", captor.getValue().getHomeCountry());
    }

    @Test
    void exportToJson_shouldPreserveTripGpsCoordinates() throws Exception {
        Trip trip = Trip.builder()
                .id(1L).name("GPS Trip")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 5))
                .budget(BigDecimal.valueOf(200))
                .dailyBudgetThreshold(BigDecimal.ZERO)
                .dailyExpenseBudget(BigDecimal.ZERO)
                .latitude(40.7128).longitude(-74.0060)
                .build();

        when(categoryRepository.findAll()).thenReturn(List.of());
        when(tripRepository.findAll()).thenReturn(List.of(trip));
        when(expenseRepository.findAll()).thenReturn(List.of());
        when(plannerEventRepository.findAll()).thenReturn(List.of());
        when(appUserRepository.findAll()).thenReturn(List.of());
        when(accommodationRepository.findAll()).thenReturn(List.of());
        when(appSettingsService.getHomeCountry()).thenReturn("FR");

        ObjectWriter writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        worker.exportToJson(out);

        ArgumentCaptor<ExportDto> captor = ArgumentCaptor.forClass(ExportDto.class);
        verify(writer).writeValue(eq(out), captor.capture());

        TripExportDto exported = captor.getValue().getTrips().get(0);
        assertEquals(40.7128, exported.getLatitude());
        assertEquals(-74.0060, exported.getLongitude());
    }

    @Test
    void exportToJson_shouldHandleEmptyDatabase() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(tripRepository.findAll()).thenReturn(List.of());
        when(expenseRepository.findAll()).thenReturn(List.of());
        when(plannerEventRepository.findAll()).thenReturn(List.of());
        when(appUserRepository.findAll()).thenReturn(List.of());
        when(accommodationRepository.findAll()).thenReturn(List.of());
        when(appSettingsService.getHomeCountry()).thenReturn("FR");

        ObjectWriter writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        worker.exportToJson(out);

        ArgumentCaptor<ExportDto> captor = ArgumentCaptor.forClass(ExportDto.class);
        verify(writer).writeValue(eq(out), captor.capture());
        ExportDto payload = captor.getValue();

        assertTrue(payload.getCategories().isEmpty());
        assertTrue(payload.getTrips().isEmpty());
        assertTrue(payload.getExpenses().isEmpty());
        assertTrue(payload.getPlannerEvents().isEmpty());
        assertTrue(payload.getUsers().isEmpty());
    }

    @Test
    void exportToJson_shouldExportMultipleUsersWithRoles() throws Exception {
        AppUser admin = AppUser.builder().id(1L).username("admin").password("hash1").role("ADMIN").build();
        AppUser reporter = AppUser.builder().id(2L).username("reporter").password("hash2").role("REPORTER").build();
        AppUser guest = AppUser.builder().id(3L).username("guest").password("hash3").role("GUEST").build();

        when(categoryRepository.findAll()).thenReturn(List.of());
        when(tripRepository.findAll()).thenReturn(List.of());
        when(expenseRepository.findAll()).thenReturn(List.of());
        when(plannerEventRepository.findAll()).thenReturn(List.of());
        when(appUserRepository.findAll()).thenReturn(List.of(admin, reporter, guest));
        when(accommodationRepository.findAll()).thenReturn(List.of());
        when(appSettingsService.getHomeCountry()).thenReturn("FR");

        ObjectWriter writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        worker.exportToJson(out);

        ArgumentCaptor<ExportDto> captor = ArgumentCaptor.forClass(ExportDto.class);
        verify(writer).writeValue(eq(out), captor.capture());

        List<UserExportDto> users = captor.getValue().getUsers();
        assertEquals(3, users.size());
        assertEquals("admin", users.get(0).getUsername());
        assertEquals("ADMIN", users.get(0).getRole());
        assertEquals("reporter", users.get(1).getUsername());
        assertEquals("guest", users.get(2).getUsername());
    }

    @Test
    void exportToJson_shouldPreserveMultiDayExpenseNumberOfDays() throws Exception {
        CategoryEntity cat = CategoryEntity.builder().id(1L).name("Hotel").icon("H").color("#fff").editable(true).build();
        Trip trip = Trip.builder().id(1L).name("Trip").startDate(LocalDate.now()).endDate(LocalDate.now())
                .budget(BigDecimal.ONE).dailyBudgetThreshold(BigDecimal.ZERO).dailyExpenseBudget(BigDecimal.ZERO).build();
        Expense expense = Expense.builder().id(1L).amount(BigDecimal.valueOf(300)).date(LocalDate.now())
                .category(cat).label("Hotel 3 nuits").numberOfDays(3).isPaid(false).trip(trip).build();

        when(categoryRepository.findAll()).thenReturn(List.of(cat));
        when(tripRepository.findAll()).thenReturn(List.of(trip));
        when(expenseRepository.findAll()).thenReturn(List.of(expense));
        when(plannerEventRepository.findAll()).thenReturn(List.of());
        when(appUserRepository.findAll()).thenReturn(List.of());
        when(accommodationRepository.findAll()).thenReturn(List.of());
        when(appSettingsService.getHomeCountry()).thenReturn("FR");

        ObjectWriter writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        worker.exportToJson(out);

        ArgumentCaptor<ExportDto> captor = ArgumentCaptor.forClass(ExportDto.class);
        verify(writer).writeValue(eq(out), captor.capture());

        ExpenseExportDto exportedExpense = captor.getValue().getExpenses().get(0);
        assertEquals(3, exportedExpense.getNumberOfDays());
        assertEquals("Hotel", exportedExpense.getCategoryName());
    }

    // ========================================================================
    // IMPORT TESTS
    // ========================================================================

    @Test
    void importFromJson_shouldRestoreHomeCountry() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of())
                .expenses(List.of())
                .plannerEvents(List.of())
                .users(List.of())
                .homeCountry("IT")
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        verify(appSettingsService).setHomeCountry("IT");
    }

    @Test
    void importFromJson_shouldNotRestoreHomeCountryWhenNull() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of())
                .expenses(List.of())
                .plannerEvents(List.of())
                .users(List.of())
                .homeCountry(null)
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        verify(appSettingsService, never()).setHomeCountry(anyString());
    }

    @Test
    void importFromJson_shouldNotRestoreHomeCountryWhenBlank() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of())
                .expenses(List.of())
                .plannerEvents(List.of())
                .users(List.of())
                .homeCountry("   ")
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        verify(appSettingsService, never()).setHomeCountry(anyString());
    }

    @Test
    void importFromJson_shouldSkipExpenseWithUnknownCategory() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(1L).name("Trip").startDate(LocalDate.now()).endDate(LocalDate.now())
                        .budget(BigDecimal.ONE).build()))
                .expenses(List.of(
                        ExpenseExportDto.builder()
                                .amount(BigDecimal.TEN).date(LocalDate.now())
                                .categoryName("Nonexistent").label("Skipped").tripId(1L).build(),
                        ExpenseExportDto.builder()
                                .amount(BigDecimal.valueOf(5)).date(LocalDate.now())
                                .categoryName("Known").label("Kept").tripId(1L).build()))
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("Nonexistent")).thenThrow(new EntityNotFoundException("not found"));
        CategoryEntity known = CategoryEntity.builder().id(2L).name("Known").editable(true).build();
        when(categoryService.findByName("Known")).thenReturn(known);
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0); t.setId(100L); return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        ArgumentCaptor<List<Expense>> captor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("Kept", captor.getValue().get(0).getLabel());
    }

    @Test
    void importFromJson_shouldHandleMultipleTripsReferencingSameCategory() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of(CategoryExportDto.builder()
                        .name("Food").icon("F").color("#aaa").editable(true).build()))
                .trips(List.of(
                        TripExportDto.builder().id(1L).name("Trip1").startDate(LocalDate.now())
                                .endDate(LocalDate.now()).budget(BigDecimal.ONE).build(),
                        TripExportDto.builder().id(2L).name("Trip2").startDate(LocalDate.now())
                                .endDate(LocalDate.now()).budget(BigDecimal.TEN).build()))
                .expenses(List.of(
                        ExpenseExportDto.builder().amount(BigDecimal.valueOf(10)).date(LocalDate.now())
                                .categoryName("Food").label("Lunch").tripId(1L).build(),
                        ExpenseExportDto.builder().amount(BigDecimal.valueOf(15)).date(LocalDate.now())
                                .categoryName("Food").label("Dinner").tripId(2L).build()))
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        CategoryEntity existing = CategoryEntity.builder().id(5L).name("Food").editable(true).build();
        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("Food")).thenReturn(existing);

        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId("Trip1".equals(t.getName()) ? 101L : 102L);
            return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        ArgumentCaptor<List<Expense>> captor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository).saveAll(captor.capture());
        List<Expense> saved = captor.getValue();
        assertEquals(2, saved.size());
        // Both expenses should reference the same category
        assertSame(saved.get(0).getCategory(), saved.get(1).getCategory());
        // But different trips
        assertNotEquals(saved.get(0).getTrip().getId(), saved.get(1).getTrip().getId());
    }

    @Test
    void importFromJson_shouldDefaultNumberOfDaysToOneWhenNull() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(1L).name("Trip").startDate(LocalDate.now()).endDate(LocalDate.now())
                        .budget(BigDecimal.ONE).build()))
                .expenses(List.of(ExpenseExportDto.builder()
                        .amount(BigDecimal.TEN).date(LocalDate.now())
                        .categoryName("Cat").label("Item").numberOfDays(null).tripId(1L).build()))
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        CategoryEntity cat = CategoryEntity.builder().id(1L).name("Cat").editable(true).build();
        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("Cat")).thenReturn(cat);
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0); t.setId(50L); return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        ArgumentCaptor<List<Expense>> captor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().get(0).getNumberOfDays());
    }

    @Test
    void importFromJson_shouldDefaultDailyBudgetThresholdToZeroWhenNull() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(1L).name("Old Trip").startDate(LocalDate.now()).endDate(LocalDate.now())
                        .budget(BigDecimal.valueOf(500))
                        .dailyBudgetThreshold(null)
                        .dailyExpenseBudget(null)
                        .build()))
                .expenses(List.of())
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0); t.setId(1L); return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        ArgumentCaptor<Trip> captor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(captor.capture());
        assertEquals(BigDecimal.ZERO, captor.getValue().getDailyBudgetThreshold());
        assertEquals(BigDecimal.ZERO, captor.getValue().getDailyExpenseBudget());
    }

    @Test
    void importFromJson_shouldWrapExpenseSaveFailureIntoIOException() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(1L).name("Trip").startDate(LocalDate.now()).endDate(LocalDate.now())
                        .budget(BigDecimal.ONE).build()))
                .expenses(List.of(ExpenseExportDto.builder()
                        .amount(BigDecimal.TEN).date(LocalDate.now())
                        .categoryName("Cat").label("Item").tripId(1L).build()))
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        CategoryEntity cat = CategoryEntity.builder().id(1L).name("Cat").editable(true).build();
        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("Cat")).thenReturn(cat);
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0); t.setId(1L); return t;
        });
        doThrow(new RuntimeException("constraint violation")).when(expenseRepository).saveAll(anyList());

        IOException ex = assertThrows(IOException.class,
                () -> worker.importFromJson(new ByteArrayInputStream("{}".getBytes())));
        assertTrue(ex.getMessage().toLowerCase().contains("dépenses"));
    }

    @Test
    void importFromJson_shouldWrapCategoryCreationFailureIntoIOException() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of(CategoryExportDto.builder()
                        .name("Broken").icon("X").color("#000").editable(true).build()))
                .trips(List.of())
                .expenses(List.of())
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("Broken")).thenThrow(new EntityNotFoundException("not found"));
        when(categoryRepository.save(any(CategoryEntity.class))).thenThrow(new RuntimeException("unique constraint"));

        IOException ex = assertThrows(IOException.class,
                () -> worker.importFromJson(new ByteArrayInputStream("{}".getBytes())));
        assertTrue(ex.getMessage().contains("catégorie"));
    }

    @Test
    void importFromJson_shouldSilentlyHandleUserImportFailure() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of())
                .expenses(List.of())
                .plannerEvents(List.of())
                .users(List.of(
                        UserExportDto.builder().id(1L).username("good").password("h1").role("ADMIN").build(),
                        UserExportDto.builder().id(2L).username("bad").password("h2").role("REPORTER").build()))
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(appUserRepository.save(any())).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            if ("bad".equals(u.getUsername())) {
                throw new RuntimeException("duplicate key");
            }
            return u;
        });

        // Should NOT throw — user import failure is silently caught
        assertDoesNotThrow(() -> worker.importFromJson(new ByteArrayInputStream("{}".getBytes())));
        verify(appUserRepository).deleteAll();
        verify(appUserRepository, times(2)).save(any(AppUser.class));
    }

    @Test
    void importFromJson_shouldImportPlannerEventsWithGpsCoordinates() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(1L).name("Trip").startDate(LocalDate.now()).endDate(LocalDate.now())
                        .budget(BigDecimal.ONE).build()))
                .expenses(List.of())
                .plannerEvents(List.of(
                        PlannerEventExportDto.builder()
                                .name("Museum").eventDateTime(LocalDateTime.of(2026, 5, 1, 10, 0))
                                .location("Louvre, Paris").latitude(48.8606).longitude(2.3376).tripId(1L).build(),
                        PlannerEventExportDto.builder()
                                .name("Lunch").eventDateTime(LocalDateTime.of(2026, 5, 1, 12, 0))
                                .location("Restaurant").latitude(null).longitude(null).tripId(1L).build()))
                .users(List.of())
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0); t.setId(10L); return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        ArgumentCaptor<List<PlannerEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(plannerEventRepository).saveAll(captor.capture());
        List<PlannerEvent> events = captor.getValue();
        assertEquals(2, events.size());

        PlannerEvent museum = events.get(0);
        assertEquals("Museum", museum.getName());
        assertEquals(48.8606, museum.getLatitude());
        assertEquals(2.3376, museum.getLongitude());

        PlannerEvent lunch = events.get(1);
        assertEquals("Lunch", lunch.getName());
        assertNull(lunch.getLatitude());
        assertNull(lunch.getLongitude());
    }

    @Test
    void importFromJson_shouldPreservePlannerEventCommentAndLocation() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(1L).name("Trip").startDate(LocalDate.now()).endDate(LocalDate.now())
                        .budget(BigDecimal.ONE).build()))
                .expenses(List.of())
                .plannerEvents(List.of(PlannerEventExportDto.builder()
                        .name("Visit").eventDateTime(LocalDateTime.of(2026, 5, 1, 9, 0))
                        .location("123 Main St, NYC")
                        .comment("Arrivée tôt le matin")
                        .tripId(1L).build()))
                .users(List.of())
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0); t.setId(1L); return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        ArgumentCaptor<List<PlannerEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(plannerEventRepository).saveAll(captor.capture());
        PlannerEvent event = captor.getValue().get(0);
        assertEquals("123 Main St, NYC", event.getLocation());
        assertEquals("Arrivée tôt le matin", event.getComment());
    }

    @Test
    void importFromJson_shouldHandleLargeNumberOfExpenses() throws Exception {
        // Import 100 expenses across 5 trips with 3 categories
        List<CategoryExportDto> cats = List.of(
                CategoryExportDto.builder().name("A").icon("A").color("#a").editable(true).build(),
                CategoryExportDto.builder().name("B").icon("B").color("#b").editable(true).build(),
                CategoryExportDto.builder().name("C").icon("C").color("#c").editable(true).build());

        List<TripExportDto> trips = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            trips.add(TripExportDto.builder()
                    .id((long) i).name("Trip " + i)
                    .startDate(LocalDate.of(2026, i, 1))
                    .endDate(LocalDate.of(2026, i, 10))
                    .budget(BigDecimal.valueOf(1000)).build());
        }

        List<ExpenseExportDto> expenses = new java.util.ArrayList<>();
        String[] catNames = {"A", "B", "C"};
        for (int i = 0; i < 100; i++) {
            expenses.add(ExpenseExportDto.builder()
                    .amount(BigDecimal.valueOf(i + 1))
                    .date(LocalDate.of(2026, (i % 5) + 1, (i % 10) + 1))
                    .categoryName(catNames[i % 3])
                    .label("Expense " + i)
                    .numberOfDays(1)
                    .tripId((long) (i % 5) + 1).build());
        }

        ExportDto dto = ExportDto.builder()
                .categories(cats).trips(trips).expenses(expenses)
                .plannerEvents(List.of()).users(List.of()).build();

        CategoryEntity catA = CategoryEntity.builder().id(1L).name("A").build();
        CategoryEntity catB = CategoryEntity.builder().id(2L).name("B").build();
        CategoryEntity catC = CategoryEntity.builder().id(3L).name("C").build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("A")).thenReturn(catA);
        when(categoryService.findByName("B")).thenReturn(catB);
        when(categoryService.findByName("C")).thenReturn(catC);

        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId((long) (trips.indexOf(trips.stream().filter(td -> td.getName().equals(t.getName())).findFirst().orElse(null)) + 1));
            return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        ArgumentCaptor<List<Expense>> captor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository).saveAll(captor.capture());
        assertEquals(100, captor.getValue().size());
    }

    @Test
    void importFromJson_shouldDeleteExistingDataBeforeImport() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(1L).name("New Trip").startDate(LocalDate.now()).endDate(LocalDate.now())
                        .budget(BigDecimal.ONE).build()))
                .expenses(List.of())
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0); t.setId(1L); return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        // Verify deletion order: accommodations first, then plannerEvents, then expenses, then trips
        var inOrder = inOrder(accommodationRepository, plannerEventRepository, expenseRepository, tripRepository);
        inOrder.verify(accommodationRepository).deleteAll();
        inOrder.verify(plannerEventRepository).deleteAll();
        inOrder.verify(expenseRepository).deleteAll();
        inOrder.verify(tripRepository).deleteAll();
    }
}
