package com.alexgit95.MyTrips.service;

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
class ImportExportWorkerTest {

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
    private CategoryService categoryService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ImportExportWorker worker;

    @Test
    void exportToJson_shouldExportOnlyCustomCategories() throws Exception {
        CategoryEntity editable = CategoryEntity.builder().id(1L).name("Transport").icon("T").color("#111").editable(true).build();
        CategoryEntity system = CategoryEntity.builder().id(2L).name("System").icon("S").color("#222").editable(false).build();

        Trip trip = Trip.builder()
                .id(10L)
                .name("Paris")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 2))
                .budget(BigDecimal.valueOf(100))
                .dailyBudgetThreshold(BigDecimal.ZERO)
                .dailyExpenseBudget(BigDecimal.ZERO)
                .build();

        Expense expense = Expense.builder()
                .id(20L)
                .amount(BigDecimal.TEN)
                .date(LocalDate.of(2026, 1, 1))
                .category(editable)
                .label("Metro")
                .numberOfDays(1)
                .trip(trip)
                .build();

        PlannerEvent event = PlannerEvent.builder()
                .id(30L)
                .name("Check-in")
                .eventDateTime(LocalDateTime.of(2026, 1, 1, 10, 0))
                .location("Paris")
                .comment("ok")
                .trip(trip)
                .build();

        AppUser user = AppUser.builder().id(40L).username("admin").password("hash").role("ADMIN").build();

        when(categoryRepository.findAll()).thenReturn(List.of(editable, system));
        when(tripRepository.findAll()).thenReturn(List.of(trip));
        when(expenseRepository.findAll()).thenReturn(List.of(expense));
        when(plannerEventRepository.findAll()).thenReturn(List.of(event));
        when(appUserRepository.findAll()).thenReturn(List.of(user));

        ObjectWriter writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        worker.exportToJson(out);

        ArgumentCaptor<ExportDto> captor = ArgumentCaptor.forClass(ExportDto.class);
        verify(writer).writeValue(eq(out), captor.capture());
        ExportDto payload = captor.getValue();

        assertEquals(1, payload.getCategories().size());
        assertEquals("Transport", payload.getCategories().get(0).getName());
        assertEquals(1, payload.getTrips().size());
        assertEquals(1, payload.getExpenses().size());
        assertEquals(1, payload.getPlannerEvents().size());
        assertEquals(1, payload.getUsers().size());
    }

    @Test
    void importFromJson_shouldImportAllDataIncludingUsers() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of(CategoryExportDto.builder().id(1L).name("Transport").icon("T").color("#111").editable(true).build()))
                .trips(List.of(TripExportDto.builder()
                        .id(100L)
                        .name("Trip A")
                        .startDate(LocalDate.of(2026, 2, 1))
                        .endDate(LocalDate.of(2026, 2, 3))
                        .budget(BigDecimal.valueOf(300))
                        .dailyBudgetThreshold(BigDecimal.valueOf(5))
                        .dailyExpenseBudget(BigDecimal.valueOf(10))
                        .latitude(48.85)
                        .longitude(2.35)
                        .build()))
                .expenses(List.of(ExpenseExportDto.builder()
                        .id(200L)
                        .amount(BigDecimal.valueOf(25))
                        .date(LocalDate.of(2026, 2, 1))
                        .categoryName("Transport")
                        .label("Train")
                        .numberOfDays(1)
                        .tripId(100L)
                        .build()))
                .plannerEvents(List.of(PlannerEventExportDto.builder()
                        .id(300L)
                        .name("Hotel")
                        .eventDateTime(LocalDateTime.of(2026, 2, 1, 20, 0))
                        .location("Paris")
                        .comment("Late")
                        .tripId(100L)
                        .build()))
                .users(List.of(UserExportDto.builder().id(400L).username("reporter").password("hash2").role("REPORTER").build()))
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("Transport")).thenThrow(new EntityNotFoundException("missing"));

        CategoryEntity createdCategory = CategoryEntity.builder().id(10L).name("Transport").icon("T").color("#111").editable(true).build();
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(createdCategory);

        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setId(999L);
            return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        verify(plannerEventRepository).deleteAll();
        verify(expenseRepository).deleteAll();
        verify(tripRepository).deleteAll();

        ArgumentCaptor<List<Expense>> expensesCaptor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository).saveAll(expensesCaptor.capture());
        assertEquals(1, expensesCaptor.getValue().size());
        assertEquals("Transport", expensesCaptor.getValue().get(0).getCategory().getName());
        assertEquals(999L, expensesCaptor.getValue().get(0).getTrip().getId());

        ArgumentCaptor<List<PlannerEvent>> plannerCaptor = ArgumentCaptor.forClass(List.class);
        verify(plannerEventRepository).saveAll(plannerCaptor.capture());
        assertEquals(1, plannerCaptor.getValue().size());
        assertEquals(999L, plannerCaptor.getValue().get(0).getTrip().getId());

        verify(appUserRepository).deleteAll();
        verify(appUserRepository).save(argThat(u -> "reporter".equals(u.getUsername()) && "hash2".equals(u.getPassword())));
    }

    @Test
    void importFromJson_shouldReuseExistingCategoryWithoutCreatingNewOne() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of(CategoryExportDto.builder().name("Food").editable(true).build()))
                .trips(List.of(TripExportDto.builder()
                        .id(1L)
                        .name("T")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .budget(BigDecimal.ONE)
                        .build()))
                .expenses(List.of(ExpenseExportDto.builder()
                        .amount(BigDecimal.ONE)
                        .date(LocalDate.now())
                        .categoryName("Food")
                        .label("Meal")
                        .tripId(1L)
                        .build()))
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        CategoryEntity existing = CategoryEntity.builder().id(22L).name("Food").editable(true).build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("Food")).thenReturn(existing);
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setId(2L);
            return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        verify(categoryRepository, never()).save(any(CategoryEntity.class));
        verify(expenseRepository).saveAll(anyList());
    }

    @Test
    void importFromJson_shouldUseFallbackCategoryLookupWhenNotInImportedCategories() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(11L)
                        .name("Trip")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .budget(BigDecimal.ONE)
                        .build()))
                .expenses(List.of(ExpenseExportDto.builder()
                        .amount(BigDecimal.ONE)
                        .date(LocalDate.now())
                        .categoryName("FallbackCategory")
                        .label("Item")
                        .tripId(11L)
                        .build()))
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        CategoryEntity fallback = CategoryEntity.builder().id(44L).name("FallbackCategory").editable(true).build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("FallbackCategory")).thenReturn(fallback);
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setId(111L);
            return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        verify(categoryService).findByName("FallbackCategory");
                verify(expenseRepository).saveAll(argThat(iterable -> {
                        List<Expense> list = toList(iterable);
                        return list.size() == 1 && "FallbackCategory".equals(list.get(0).getCategory().getName());
                }));
    }

    @Test
    void importFromJson_shouldWrapTripImportFailuresIntoIOException() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(5L)
                        .name("Broken")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .budget(BigDecimal.ONE)
                        .build()))
                .expenses(List.of())
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(tripRepository.save(any(Trip.class))).thenThrow(new RuntimeException("db down"));

        IOException ex = assertThrows(IOException.class,
                () -> worker.importFromJson(new ByteArrayInputStream("{}".getBytes())));

        assertTrue(ex.getMessage().toLowerCase().contains("importation des voyages"));
    }

    @Test
    void importFromJson_shouldPropagateReadIOException() throws Exception {
        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class)))
                .thenThrow(new IOException("invalid json"));

        IOException ex = assertThrows(IOException.class,
                () -> worker.importFromJson(new ByteArrayInputStream("{}".getBytes())));

        assertEquals("invalid json", ex.getMessage());
    }

    @Test
    void importFromJson_shouldHandleNullSectionsGracefully() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(null)
                .trips(null)
                .expenses(null)
                .plannerEvents(null)
                .users(null)
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        verify(plannerEventRepository).deleteAll();
        verify(expenseRepository).deleteAll();
        verify(tripRepository).deleteAll();
        verify(expenseRepository).saveAll(argThat(iterable -> toList(iterable).isEmpty()));
        verify(plannerEventRepository, never()).saveAll(anyList());
        verify(appUserRepository, never()).deleteAll();
    }

    @Test
    void importFromJson_shouldSkipOrphanExpenseAndPlannerEvent() throws Exception {
        ExportDto dto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(10L)
                        .name("Trip")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .budget(BigDecimal.valueOf(10))
                        .build()))
                .expenses(List.of(
                        ExpenseExportDto.builder()
                                .amount(BigDecimal.ONE)
                                .date(LocalDate.now())
                                .categoryName("KnownCategory")
                                .label("ok")
                                .tripId(10L)
                                .build(),
                        ExpenseExportDto.builder()
                                .amount(BigDecimal.ONE)
                                .date(LocalDate.now())
                                .categoryName("KnownCategory")
                                .label("orphan")
                                .tripId(999L)
                                .build()))
                .plannerEvents(List.of(
                        PlannerEventExportDto.builder()
                                .name("valid")
                                .eventDateTime(LocalDateTime.now())
                                .tripId(10L)
                                .build(),
                        PlannerEventExportDto.builder()
                                .name("orphan")
                                .eventDateTime(LocalDateTime.now())
                                .tripId(999L)
                                .build()))
                .users(List.of())
                .build();

        CategoryEntity fallback = CategoryEntity.builder().id(44L).name("KnownCategory").editable(true).build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(dto);
        when(categoryService.findByName("KnownCategory")).thenReturn(fallback);
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setId(101L);
            return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

                verify(expenseRepository).saveAll(argThat(iterable -> {
                        List<Expense> list = toList(iterable);
                        return list.size() == 1 && "ok".equals(list.get(0).getLabel());
                }));
                verify(plannerEventRepository).saveAll(argThat(iterable -> {
                        List<PlannerEvent> list = toList(iterable);
                        return list.size() == 1 && "valid".equals(list.get(0).getName());
                }));
    }

    @Test
    void exportAndImport_shouldPreserveIsPaidStatus() throws Exception {
        // Given: Une dépense avec isPaid = true
        CategoryEntity category = CategoryEntity.builder().id(1L).name("Transport").icon("T").color("#111").editable(true).build();
        Trip trip = Trip.builder()
                .id(10L)
                .name("Paris")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 2))
                .budget(BigDecimal.valueOf(100))
                .build();

        Expense expenseWithPaid = Expense.builder()
                .id(20L)
                .amount(BigDecimal.TEN)
                .date(LocalDate.of(2026, 1, 1))
                .category(category)
                .label("Metro")
                .numberOfDays(1)
                .trip(trip)
                .isPaid(true)
                .build();

        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(tripRepository.findAll()).thenReturn(List.of(trip));
        when(expenseRepository.findAll()).thenReturn(List.of(expenseWithPaid));
        when(plannerEventRepository.findAll()).thenReturn(List.of());
        when(appUserRepository.findAll()).thenReturn(List.of());

        ObjectWriter writer = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);

        // When: Export
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        worker.exportToJson(out);

        // Then: Vérifier que isPaid = true est présent dans l'export
        ArgumentCaptor<ExportDto> exportCaptor = ArgumentCaptor.forClass(ExportDto.class);
        verify(writer).writeValue(eq(out), exportCaptor.capture());
        ExportDto exported = exportCaptor.getValue();

        assertEquals(1, exported.getExpenses().size());
        assertTrue(exported.getExpenses().get(0).getIsPaid());

        // When: Import
        ExportDto importDto = ExportDto.builder()
                .categories(List.of(CategoryExportDto.builder().id(1L).name("Transport").icon("T").color("#111").editable(true).build()))
                .trips(List.of(TripExportDto.builder()
                        .id(10L)
                        .name("Paris")
                        .startDate(LocalDate.of(2026, 1, 1))
                        .endDate(LocalDate.of(2026, 1, 2))
                        .budget(BigDecimal.valueOf(100))
                        .build()))
                .expenses(List.of(ExpenseExportDto.builder()
                        .id(20L)
                        .amount(BigDecimal.TEN)
                        .date(LocalDate.of(2026, 1, 1))
                        .categoryName("Transport")
                        .label("Metro")
                        .numberOfDays(1)
                        .isPaid(true)
                        .tripId(10L)
                        .build()))
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(importDto);
        when(categoryService.findByName("Transport")).thenThrow(new EntityNotFoundException("missing"));

        CategoryEntity createdCategory = CategoryEntity.builder().id(1L).name("Transport").icon("T").color("#111").editable(true).build();
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(createdCategory);

        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setId(999L);
            return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        // Then: Vérifier que isPaid = true a été conservé lors de l'import
        ArgumentCaptor<List<Expense>> importCaptor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository).saveAll(importCaptor.capture());
        assertEquals(1, importCaptor.getValue().size());
        assertTrue(importCaptor.getValue().get(0).getIsPaid(), "isPaid should be preserved as true after import");
    }

    @Test
    void importFromJson_shouldDefaultIsPaidToFalseForMissingValue() throws Exception {
        // Given: une dépense sans isPaid dans l'export (rétrocompatibilité)
        ExportDto importDto = ExportDto.builder()
                .categories(List.of())
                .trips(List.of(TripExportDto.builder()
                        .id(10L)
                        .name("Old Trip")
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .budget(BigDecimal.ONE)
                        .build()))
                .expenses(List.of(ExpenseExportDto.builder()
                        .amount(BigDecimal.TEN)
                        .date(LocalDate.now())
                        .categoryName("Food")
                        .label("Meal")
                        .tripId(10L)
                        // isPaid is not set (null)
                        .build()))
                .plannerEvents(List.of())
                .users(List.of())
                .build();

        CategoryEntity category = CategoryEntity.builder().id(1L).name("Food").editable(true).build();

        when(objectMapper.readValue(any(ByteArrayInputStream.class), eq(ExportDto.class))).thenReturn(importDto);
        when(categoryService.findByName("Food")).thenReturn(category);
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip t = invocation.getArgument(0);
            t.setId(777L);
            return t;
        });

        worker.importFromJson(new ByteArrayInputStream("{}".getBytes()));

        // Then: isPaid devrait être false par défaut
        ArgumentCaptor<List<Expense>> captor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertFalse(captor.getValue().get(0).getIsPaid(), "isPaid should default to false when missing");
    }

        private <T> List<T> toList(Iterable<T> iterable) {
                List<T> list = new java.util.ArrayList<>();
                for (T item : iterable) {
                        list.add(item);
                }
                return list;
        }
}
