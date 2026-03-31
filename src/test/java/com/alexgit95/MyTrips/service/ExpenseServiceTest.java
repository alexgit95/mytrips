package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.ChartDataDto;
import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService service;

    @Test
    void findById_shouldThrowWhenExpenseIsMissing() {
        when(expenseRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findById(123L));
    }

    @Test
    void buildChartData_shouldComputeProjectionForNotStartedTrip() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        Trip trip = Trip.builder()
                .id(7L)
                .name("Future")
                .startDate(start)
                .endDate(end)
                .budget(BigDecimal.valueOf(100))
                .dailyExpenseBudget(BigDecimal.valueOf(10))
                .build();

        CategoryEntity category = CategoryEntity.builder().name("Transport").icon("T").build();

        Expense e1 = Expense.builder()
                .amount(BigDecimal.valueOf(9))
                .date(start)
                .numberOfDays(1)
                .category(category)
                .label("Taxi")
                .trip(trip)
                .build();

        Expense e2 = Expense.builder()
                .amount(BigDecimal.valueOf(20))
                .date(start.plusDays(1))
                .numberOfDays(2)
                .category(category)
                .label("Hotel")
                .trip(trip)
                .build();

        when(expenseRepository.findByTripIdOrderByDateAsc(7L)).thenReturn(List.of(e1, e2));
        when(expenseRepository.sumByCategory(7L)).thenReturn(Collections.singletonList(new Object[]{category, 29.0}));

        ChartDataDto data = service.buildChartData(trip);

        assertEquals(3, data.getLabels().size());
        assertEquals(List.of(100.0, 100.0, 100.0), data.getBudgetLine());
        assertEquals(Arrays.asList(null, null, null), data.getActualCumulative());
        assertEquals(List.of(19.0, 39.0, 59.0), data.getTrendLine());
        assertEquals(List.of("T Transport"), data.getCategoryLabels());
        assertEquals(List.of(29.0), data.getCategoryAmounts());
    }

    @Test
    void totalByTrip_shouldDelegateToRepository() {
        when(expenseRepository.sumAmountByTripId(5L)).thenReturn(BigDecimal.valueOf(42));

        BigDecimal total = service.totalByTrip(5L);

        assertEquals(BigDecimal.valueOf(42), total);
    }
}
