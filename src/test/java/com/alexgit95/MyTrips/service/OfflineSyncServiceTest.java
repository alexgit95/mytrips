package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.OfflineAction;
import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfflineSyncServiceTest {

    @Mock
    private TripService tripService;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private PlannerEventService plannerEventService;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private OfflineSyncService offlineSyncService;

    // ─── Expense Tests ──────────────────────────────────────────────

    @Test
    void processAction_shouldCreateExpense() {
        Trip trip = Trip.builder().id(1L).name("Test Trip")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(5))
                .budget(BigDecimal.valueOf(1000)).build();
        CategoryEntity category = CategoryEntity.builder().id(2L).name("Food").build();

        when(tripService.findById(1L)).thenReturn(trip);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(expenseService.findByTrip(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> data = new HashMap<>();
        data.put("label", "Restaurant");
        data.put("amount", 25.50);
        data.put("date", "2026-05-19");
        data.put("categoryId", "2");
        data.put("numberOfDays", 1);
        data.put("isPaid", false);

        OfflineAction action = new OfflineAction("expense", 1L, "2026-05-19T12:00:00Z", data);

        offlineSyncService.processAction(action);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseService).save(captor.capture());

        Expense saved = captor.getValue();
        assertEquals("Restaurant", saved.getLabel());
        assertEquals(0, BigDecimal.valueOf(25.50).compareTo(saved.getAmount()));
        assertEquals(LocalDate.of(2026, 5, 19), saved.getDate());
        assertEquals(category, saved.getCategory());
        assertEquals(1, saved.getNumberOfDays());
        assertFalse(saved.getIsPaid());
    }

    @Test
    void processAction_shouldSkipDuplicateExpense() {
        Trip trip = Trip.builder().id(1L).name("Test Trip")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(5))
                .budget(BigDecimal.valueOf(1000)).build();
        CategoryEntity category = CategoryEntity.builder().id(2L).name("Food").build();

        Expense existing = Expense.builder()
                .label("Restaurant")
                .amount(BigDecimal.valueOf(25.50))
                .date(LocalDate.of(2026, 5, 19))
                .category(category)
                .trip(trip)
                .build();

        when(tripService.findById(1L)).thenReturn(trip);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(expenseService.findByTrip(1L)).thenReturn(List.of(existing));

        Map<String, Object> data = new HashMap<>();
        data.put("label", "Restaurant");
        data.put("amount", 25.50);
        data.put("date", "2026-05-19");
        data.put("categoryId", "2");
        data.put("numberOfDays", 1);
        data.put("isPaid", false);

        OfflineAction action = new OfflineAction("expense", 1L, "2026-05-19T12:00:00Z", data);

        offlineSyncService.processAction(action);

        verify(expenseService, never()).save(any());
    }

    // ─── PlannerEvent Tests ─────────────────────────────────────────

    @Test
    void processAction_shouldCreatePlannerEvent() {
        Trip trip = Trip.builder().id(1L).name("Test Trip")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(5))
                .budget(BigDecimal.valueOf(1000)).build();

        when(tripService.findById(1L)).thenReturn(trip);
        when(plannerEventService.findByTrip(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Visite musée");
        data.put("eventDateTime", "2026-05-19T14:30:00");
        data.put("location", "Rue du Louvre, Paris");
        data.put("latitude", 48.8606);
        data.put("longitude", 2.3376);
        data.put("comment", "Réserver billets");

        OfflineAction action = new OfflineAction("planner_event", 1L, "2026-05-19T12:00:00Z", data);

        offlineSyncService.processAction(action);

        ArgumentCaptor<PlannerEvent> captor = ArgumentCaptor.forClass(PlannerEvent.class);
        verify(plannerEventService).create(eq(1L), captor.capture());

        PlannerEvent saved = captor.getValue();
        assertEquals("Visite musée", saved.getName());
        assertEquals(LocalDateTime.of(2026, 5, 19, 14, 30, 0), saved.getEventDateTime());
        assertEquals("Rue du Louvre, Paris", saved.getLocation());
        assertEquals(48.8606, saved.getLatitude());
        assertEquals(2.3376, saved.getLongitude());
        assertEquals("Réserver billets", saved.getComment());
    }

    @Test
    void processAction_shouldSkipDuplicatePlannerEvent() {
        Trip trip = Trip.builder().id(1L).name("Test Trip")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(5))
                .budget(BigDecimal.valueOf(1000)).build();

        PlannerEvent existing = PlannerEvent.builder()
                .name("Visite musée")
                .eventDateTime(LocalDateTime.of(2026, 5, 19, 14, 30, 0))
                .trip(trip)
                .build();

        when(tripService.findById(1L)).thenReturn(trip);
        when(plannerEventService.findByTrip(1L)).thenReturn(List.of(existing));

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Visite musée");
        data.put("eventDateTime", "2026-05-19T14:30:00");
        data.put("location", "Rue du Louvre, Paris");
        data.put("latitude", null);
        data.put("longitude", null);
        data.put("comment", "");

        OfflineAction action = new OfflineAction("planner_event", 1L, "2026-05-19T12:00:00Z", data);

        offlineSyncService.processAction(action);

        verify(plannerEventService, never()).create(anyLong(), any());
    }

    @Test
    void processAction_shouldThrowOnUnknownType() {
        Map<String, Object> data = new HashMap<>();
        OfflineAction action = new OfflineAction("unknown", 1L, "2026-05-19T12:00:00Z", data);

        assertThrows(IllegalArgumentException.class, () -> offlineSyncService.processAction(action));
    }

    @Test
    void processAction_shouldCreatePlannerEventWithoutCoordinates() {
        Trip trip = Trip.builder().id(1L).name("Test Trip")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(5))
                .budget(BigDecimal.valueOf(1000)).build();

        when(tripService.findById(1L)).thenReturn(trip);
        when(plannerEventService.findByTrip(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Déjeuner");
        data.put("eventDateTime", "2026-05-20T12:00:00");
        data.put("location", "");
        data.put("latitude", null);
        data.put("longitude", null);
        data.put("comment", "");

        OfflineAction action = new OfflineAction("planner_event", 1L, "2026-05-19T12:00:00Z", data);

        offlineSyncService.processAction(action);

        ArgumentCaptor<PlannerEvent> captor = ArgumentCaptor.forClass(PlannerEvent.class);
        verify(plannerEventService).create(eq(1L), captor.capture());

        PlannerEvent saved = captor.getValue();
        assertEquals("Déjeuner", saved.getName());
        assertNull(saved.getLatitude());
        assertNull(saved.getLongitude());
    }
}
