package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.OfflineAction;
import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service de synchronisation des actions créées hors ligne.
 * Politique de conflit : la valeur serveur a priorité.
 * Seules les créations sont acceptées (pas de mise à jour d'entités existantes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineSyncService {

    private final TripService tripService;
    private final ExpenseService expenseService;
    private final PlannerEventService plannerEventService;
    private final CategoryRepository categoryRepository;

    @Transactional
    public void processAction(OfflineAction action) {
        switch (action.getType()) {
            case "expense" -> processExpense(action);
            case "planner_event" -> processPlannerEvent(action);
            default -> throw new IllegalArgumentException("Type d'action inconnu : " + action.getType());
        }
    }

    private void processExpense(OfflineAction action) {
        Trip trip = tripService.findById(action.getTripId());
        Map<String, Object> data = action.getData();

        Expense expense = new Expense();
        expense.setTrip(trip);
        expense.setLabel((String) data.get("label"));
        expense.setAmount(toBigDecimal(data.get("amount")));
        expense.setDate(LocalDate.parse((String) data.get("date")));

        // Resolve category
        Object categoryIdObj = data.get("categoryId");
        if (categoryIdObj != null) {
            Long categoryId = toLong(categoryIdObj);
            CategoryEntity category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable : " + categoryId));
            expense.setCategory(category);
        }

        Object numberOfDaysObj = data.get("numberOfDays");
        if (numberOfDaysObj != null) {
            expense.setNumberOfDays(toInt(numberOfDaysObj));
        }

        Object isPaidObj = data.get("isPaid");
        if (isPaidObj instanceof Boolean) {
            expense.setIsPaid((Boolean) isPaidObj);
        }

        // Conflict policy: if an identical expense already exists (same trip, label, amount, date),
        // server value wins → skip this offline entry
        boolean duplicate = expenseService.findByTrip(trip.getId()).stream()
                .anyMatch(e -> e.getLabel().equals(expense.getLabel())
                        && e.getAmount().compareTo(expense.getAmount()) == 0
                        && e.getDate().equals(expense.getDate()));

        if (duplicate) {
            log.info("[OfflineSync] Skipping duplicate expense: {} on {} for trip {}",
                    expense.getLabel(), expense.getDate(), trip.getId());
            return;
        }

        expenseService.save(expense);
        log.info("[OfflineSync] Saved expense: {} for trip {}", expense.getLabel(), trip.getId());
    }

    private void processPlannerEvent(OfflineAction action) {
        Trip trip = tripService.findById(action.getTripId());
        Map<String, Object> data = action.getData();

        PlannerEvent event = new PlannerEvent();
        event.setName((String) data.get("name"));
        event.setEventDateTime(LocalDateTime.parse((String) data.get("eventDateTime")));
        event.setLocation((String) data.get("location"));
        event.setComment((String) data.get("comment"));

        Object lat = data.get("latitude");
        Object lng = data.get("longitude");
        if (lat != null) event.setLatitude(toDouble(lat));
        if (lng != null) event.setLongitude(toDouble(lng));

        // Conflict policy: if an identical event already exists (same trip, name, dateTime),
        // server value wins → skip this offline entry
        boolean duplicate = plannerEventService.findByTrip(trip.getId()).stream()
                .anyMatch(e -> e.getName().equals(event.getName())
                        && e.getEventDateTime().equals(event.getEventDateTime()));

        if (duplicate) {
            log.info("[OfflineSync] Skipping duplicate planner event: {} on {} for trip {}",
                    event.getName(), event.getEventDateTime(), trip.getId());
            return;
        }

        plannerEventService.create(trip.getId(), event);
        log.info("[OfflineSync] Saved planner event: {} for trip {}", event.getName(), trip.getId());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}
