package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.ExportDto;
import com.alexgit95.MyTrips.dto.ExpenseExportDto;
import com.alexgit95.MyTrips.dto.TripExportDto;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.ExpenseRepository;
import com.alexgit95.MyTrips.repository.TripRepository;
import jakarta.persistence.EntityNotFoundException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
