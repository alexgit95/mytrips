package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.ExportDto;
import com.alexgit95.MyTrips.dto.ExpenseExportDto;
import com.alexgit95.MyTrips.dto.TripExportDto;
import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.ExpenseRepository;
import com.alexgit95.MyTrips.repository.TripRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DataImportExportService {

    private final TripRepository     tripRepository;
    private final ExpenseRepository  expenseRepository;
    private final CategoryService    categoryService;
    private final ObjectMapper       objectMapper;

    // ----------------------------------------------------------------
    // Export: serialise toutes les données en JSON vers l'OutputStream
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public void exportToJson(OutputStream out) throws IOException {

        List<Trip>    trips    = tripRepository.findAll();
        List<Expense> expenses = expenseRepository.findAll();

        List<TripExportDto> tripDtos = trips.stream()
                .map(t -> TripExportDto.builder()
                        .id(t.getId()).name(t.getName())
                        .startDate(t.getStartDate()).endDate(t.getEndDate())
                        .budget(t.getBudget()).imageUrl(t.getImageUrl())
                        .dailyBudgetThreshold(t.getDailyBudgetThreshold())
                        .dailyExpenseBudget(t.getDailyExpenseBudget())
                        .latitude(t.getLatitude()).longitude(t.getLongitude())
                        .build())
                .toList();

        List<ExpenseExportDto> expenseDtos = expenses.stream()
                .map(e -> ExpenseExportDto.builder()
                        .id(e.getId()).amount(e.getAmount())
                        .date(e.getDate()).categoryName(e.getCategory().getName())
                        .label(e.getLabel()).numberOfDays(e.getNumberOfDays())
                        .tripId(e.getTrip().getId())
                        .build())
                .toList();

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(out, ExportDto.builder()
                        .trips(tripDtos).expenses(expenseDtos).build());
    }

    // ----------------------------------------------------------------
    // Import: remplace toutes les données par celles du JSON fourni
    // ----------------------------------------------------------------
    public void importFromJson(InputStream in) throws IOException {

        ExportDto data = objectMapper.readValue(in, ExportDto.class);

        // Suppression dans l'ordre pour respecter les FK
        expenseRepository.deleteAll();
        tripRepository.deleteAll();
        expenseRepository.flush();
        tripRepository.flush();

        // Créer les voyages et conserver le mapping ancienId → nouvelle entité
        Map<Long, Trip> tripById = new HashMap<>();
        for (TripExportDto dto : data.getTrips()) {
            Trip saved = tripRepository.save(Trip.builder()
                    .name(dto.getName()).startDate(dto.getStartDate())
                    .endDate(dto.getEndDate()).budget(dto.getBudget())
                    .imageUrl(dto.getImageUrl())
                    .dailyBudgetThreshold(dto.getDailyBudgetThreshold() != null ? dto.getDailyBudgetThreshold() : java.math.BigDecimal.ZERO)
                    .dailyExpenseBudget(dto.getDailyExpenseBudget() != null ? dto.getDailyExpenseBudget() : java.math.BigDecimal.ZERO)
                    .latitude(dto.getLatitude()).longitude(dto.getLongitude())
                    .build());
            tripById.put(dto.getId(), saved);
        }

        // Créer les dépenses en reliant aux nouveaux voyages et catégories
        List<Expense> toSave = new ArrayList<>();
        for (ExpenseExportDto dto : data.getExpenses()) {
            Trip trip = tripById.get(dto.getTripId());
            if (trip == null) continue;
            try {
                var category = categoryService.findByName(dto.getCategoryName());
                toSave.add(Expense.builder()
                        .amount(dto.getAmount()).date(dto.getDate())
                        .category(category).label(dto.getLabel())
                        .numberOfDays(dto.getNumberOfDays() != null ? dto.getNumberOfDays() : 1)
                        .trip(trip).build());
            } catch (EntityNotFoundException e) {
                // Catégorie non trouvée lors de l'import : ignorer cette dépense ou logger
                System.err.println("Catégorie manquante lors de l'import : " + dto.getCategoryName());
            }
        }
        expenseRepository.saveAll(toSave);
    }

    // ----------------------------------------------------------------
    // Import from HopWallet CSV
    // ----------------------------------------------------------------
    public void importFromHopWalletCsv(InputStream in) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(in))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Fichier CSV vide");
            }

            // Skip header
            rows.remove(0);

            // Map HopWallet categories to MyTrips categories
            Map<String, String> categoryMapping = Map.of(
                "Hôtel", "Hébergement",
                "Restauration", "Restauration",
                "Goûter", "Loisirs",
                "Sorties", "Sorties",
                "Transport", "Transport",
                "Voiture & Parking", "Transport",
                "Courses", "Courses"
            );

            // Group expenses by trip
            Map<String, List<HopWalletExpense>> tripExpenses = new HashMap<>();
            Map<String, HopWalletTrip> trips = new HashMap<>();

            for (String[] row : rows) {
                if (row.length < 17) continue; // Ensure enough columns

                String tripName = row[1].trim();
                if (tripName.isEmpty()) continue;

                // Some exports may have missing dates; ignore these rows rather than fail the whole import.
                String startDateStr = row[3].trim();
                String endDateStr = row[4].trim();
                String expenseDateStr = row[7].trim();
                if (expenseDateStr.isEmpty()) continue;

                LocalDate startDate;
                LocalDate endDate;
                LocalDate expenseDate;
                try {
                    startDate = startDateStr.isEmpty() ? null : LocalDate.parse(startDateStr);
                    endDate = endDateStr.isEmpty() ? null : LocalDate.parse(endDateStr);
                    expenseDate = LocalDate.parse(expenseDateStr);
                } catch (DateTimeParseException e) {
                    // Ignore rows with invalid date formats
                    continue;
                }

                // Fallback to expense date when trip dates are missing
                if (startDate == null) startDate = expenseDate;
                if (endDate == null) endDate = expenseDate;

                BigDecimal tripBudget;
                try {
                    tripBudget = new BigDecimal(row[6].trim());
                } catch (NumberFormatException e) {
                    tripBudget = BigDecimal.ZERO;
                }

                // Local variables used in a lambda must be effectively final
                final LocalDate tripStart = startDate;
                final LocalDate tripEnd = endDate;
                final BigDecimal tripBudgetFinal = tripBudget;

                HopWalletTrip trip = trips.computeIfAbsent(tripName, k -> new HopWalletTrip(tripName, tripStart, tripEnd, tripBudgetFinal));
                trip.updateDates(tripStart, tripEnd);

                String expenseCategory = row[8];
                BigDecimal expenseAmount;
                try {
                    expenseAmount = new BigDecimal(row[9].trim());
                } catch (NumberFormatException e) {
                    continue; // skip invalid amount row
                }
                String expenseNote = row[13];

                HopWalletExpense expense = new HopWalletExpense(expenseDate, expenseCategory, expenseAmount, expenseNote);
                tripExpenses.computeIfAbsent(tripName, k -> new ArrayList<>()).add(expense);
            }

            // Create trips
            Map<String, Trip> createdTrips = new HashMap<>();
            for (HopWalletTrip hwt : trips.values()) {
                Trip trip = Trip.builder()
                    .name(hwt.name)
                    .startDate(hwt.startDate)
                    .endDate(hwt.endDate)
                    .budget(hwt.budget)
                    .dailyBudgetThreshold(BigDecimal.ZERO)
                    .dailyExpenseBudget(BigDecimal.ZERO)
                    .build();
                createdTrips.put(hwt.name, tripRepository.save(trip));
            }

            // Create expenses
            List<Expense> expensesToSave = new ArrayList<>();
            for (Map.Entry<String, List<HopWalletExpense>> entry : tripExpenses.entrySet()) {
                String tripName = entry.getKey();
                Trip trip = createdTrips.get(tripName);
                for (HopWalletExpense hwe : entry.getValue()) {
                    String mappedCategory = categoryMapping.getOrDefault(hwe.category, "Autre");
                    CategoryEntity category = categoryService.findByName(mappedCategory);
                    Expense expense = Expense.builder()
                        .amount(hwe.amount)
                        .date(hwe.date)
                        .category(category)
                        .label(hwe.note != null && !hwe.note.isEmpty() ? hwe.note : hwe.category)
                        .numberOfDays(1)
                        .trip(trip)
                        .build();
                    expensesToSave.add(expense);
                }
            }
            expenseRepository.saveAll(expensesToSave);
        }
    }

    // Helper classes
    private static class HopWalletTrip {
        String name;
        LocalDate startDate;
        LocalDate endDate;
        BigDecimal budget;

        HopWalletTrip(String name, LocalDate startDate, LocalDate endDate, BigDecimal budget) {
            this.name = name;
            this.startDate = startDate;
            this.endDate = endDate;
            this.budget = budget;
        }

        void updateDates(LocalDate start, LocalDate end) {
            if (start.isBefore(this.startDate)) this.startDate = start;
            if (end.isAfter(this.endDate)) this.endDate = end;
        }
    }

    private static class HopWalletExpense {
        LocalDate date;
        String category;
        BigDecimal amount;
        String note;

        HopWalletExpense(LocalDate date, String category, BigDecimal amount, String note) {
            this.date = date;
            this.category = category;
            this.amount = amount;
            this.note = note;
        }
    }
}
