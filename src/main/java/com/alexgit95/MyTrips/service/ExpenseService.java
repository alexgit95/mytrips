package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.ChartDataDto;
import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<Expense> findByTrip(Long tripId) {
        return expenseRepository.findByTripIdOrderByDateAsc(tripId);
    }

    @Transactional(readOnly = true)
    public Expense findById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dépense introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public BigDecimal totalByTrip(Long tripId) {
        return expenseRepository.sumAmountByTripId(tripId);
    }

    public Expense save(Expense expense) {
        return expenseRepository.save(expense);
    }

    public void delete(Long id) {
        expenseRepository.deleteById(id);
    }

    // -------------------------------------------------------------
    // Chart data computation
    // -------------------------------------------------------------
    @Transactional(readOnly = true)
    public ChartDataDto buildChartData(Trip trip) {

        List<Expense> expenses = expenseRepository.findByTripIdOrderByDateAsc(trip.getId());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

        LocalDate start = trip.getStartDate();
        LocalDate end   = trip.getEndDate();
        double budget   = trip.getBudget().doubleValue();

        // Build a list of all days in the trip
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        List<LocalDate> allDays = new ArrayList<>();
        for (long i = 0; i < totalDays; i++) {
            allDays.add(start.plusDays(i));
        }

        // Group expenses by day, accounting for multi-day expenses
        Map<LocalDate, Double> dailyExpense = new HashMap<>();
        for (Expense expense : expenses) {
            LocalDate expenseDate = expense.getDate();
            int numDays = expense.getNumberOfDays() != null ? expense.getNumberOfDays() : 1;
            double amountPerDay = expense.getAmount().doubleValue() / numDays;
            for (int day = 0; day < numDays; day++) {
                LocalDate currentDay = expenseDate.plusDays(day);
                dailyExpense.put(currentDay, dailyExpense.getOrDefault(currentDay, 0.0) + amountPerDay);
            }
        }

        // Build cumulative actual series and find last expense date
        List<Double> actualCumulative = new ArrayList<>();
        List<String> labels           = new ArrayList<>();
        List<Double> budgetLine       = new ArrayList<>();

        double cumul = 0.0;
        LocalDate lastActualDay = null;
        double lastCumul = 0.0;
        int lastActualIdx = -1;

        LocalDate today = LocalDate.now();

        for (int i = 0; i < allDays.size(); i++) {
            LocalDate day = allDays.get(i);
            labels.add(day.format(fmt));
            budgetLine.add(budget);

            if (!day.isAfter(today)) {
                cumul += dailyExpense.getOrDefault(day, 0.0);
                actualCumulative.add(cumul);
                lastActualDay = day;
                lastCumul = cumul;
                lastActualIdx = i;
            } else {
                actualCumulative.add(null);
            }
        }

        // Build trend line — configured dailyExpenseBudget as baseline +
        // any pre-entered future expenses already recorded in dailyExpense
        List<Double> trendLine = new ArrayList<>(Collections.nCopies(allDays.size(), null));

        double dailyBudget = (trip.getDailyExpenseBudget() != null)
                ? trip.getDailyExpenseBudget().doubleValue() : 0.0;

        // Check whether there are future pre-entered expenses to show
        boolean hasFutureExpenses = allDays.stream()
                .anyMatch(d -> d.isAfter(today) && dailyExpense.containsKey(d));

        if (lastActualIdx >= 0 && (dailyBudget > 0 || hasFutureExpenses)) {
            // Anchor: last actual point
            trendLine.set(lastActualIdx, lastCumul);
            double running = lastCumul;
            // Project forward: base daily rate + any pre-entered future expense on that day
            for (int i = lastActualIdx + 1; i < allDays.size(); i++) {
                LocalDate day = allDays.get(i);
                double futureEntered = dailyExpense.getOrDefault(day, 0.0);
                running += dailyBudget + futureEntered;
                trendLine.set(i, running);
            }
        }

        // Pie data: sum by category
        List<Object[]> catSums = expenseRepository.sumByCategory(trip.getId());
        List<String> categoryLabels  = new ArrayList<>();
        List<Double> categoryAmounts = new ArrayList<>();
        for (Object[] row : catSums) {
            CategoryEntity cat = (CategoryEntity) row[0];
            double   amt = ((Number) row[1]).doubleValue();
            categoryLabels.add(cat.getIcon() + " " + cat.getName());
            categoryAmounts.add(amt);
        }

        return ChartDataDto.builder()
                .labels(labels)
                .actualCumulative(actualCumulative)
                .budgetLine(budgetLine)
                .trendLine(trendLine)
                .categoryLabels(categoryLabels)
                .categoryAmounts(categoryAmounts)
                .build();
    }
}
