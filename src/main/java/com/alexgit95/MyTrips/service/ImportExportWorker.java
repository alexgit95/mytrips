package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.CategoryExportDto;
import com.alexgit95.MyTrips.dto.ExportDto;
import com.alexgit95.MyTrips.dto.ExpenseExportDto;
import com.alexgit95.MyTrips.dto.PlannerEventExportDto;
import com.alexgit95.MyTrips.dto.TripExportDto;
import com.alexgit95.MyTrips.dto.UserExportDto;
import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.AppUser;
import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.AppUserRepository;
import com.alexgit95.MyTrips.repository.CategoryRepository;
import com.alexgit95.MyTrips.repository.ExpenseRepository;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import com.alexgit95.MyTrips.repository.TripRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service d'import/export SANS aspects transactionnels
 * pour éviter les conflits avec Spring AOP
 */
@Service
@RequiredArgsConstructor
public class ImportExportWorker {

    private final CategoryRepository categoryRepository;
    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;
    private final PlannerEventRepository plannerEventRepository;
    private final AppUserRepository appUserRepository;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;

    // ----------------------------------------------------------------
    // Export: serialise toutes les données en JSON vers l'OutputStream
    // ----------------------------------------------------------------
    public void exportToJson(OutputStream out) throws IOException {
        try {
            System.out.println("[EXPORT] Démarrage de l'export JSON...");

            // Exporter uniquement les catégories custom (editable=true)
            List<CategoryEntity> customCategories = categoryRepository.findAll()
                    .stream()
                    .filter(c -> c.getEditable() != null && c.getEditable())
                    .toList();

            List<Trip> trips = tripRepository.findAll();
            List<Expense> expenses = expenseRepository.findAll();
            List<PlannerEvent> plannerEvents = plannerEventRepository.findAll();
            List<AppUser> users = appUserRepository.findAll();
            System.out.println("[EXPORT] Catégories custom trouvées : " + customCategories.size());
            System.out.println("[EXPORT] Voyages trouvés : " + trips.size());
            System.out.println("[EXPORT] Dépenses trouvées : " + expenses.size());
            System.out.println("[EXPORT] Événements planner trouvés : " + plannerEvents.size());
            System.out.println("[EXPORT] Utilisateurs trouvés : " + users.size());

            List<CategoryExportDto> categoryDtos = customCategories.stream()
                    .map(c -> CategoryExportDto.builder()
                            .id(c.getId()).name(c.getName())
                            .icon(c.getIcon()).color(c.getColor())
                            .editable(c.getEditable())
                            .build())
                    .toList();

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
                            .isPaid(e.getIsPaid())
                            .tripId(e.getTrip().getId())
                            .build())
                    .toList();

            List<PlannerEventExportDto> plannerEventDtos = plannerEvents.stream()
                    .map(pe -> PlannerEventExportDto.builder()
                            .id(pe.getId()).name(pe.getName())
                            .eventDateTime(pe.getEventDateTime())
                            .location(pe.getLocation())
                            .comment(pe.getComment())
                            .tripId(pe.getTrip().getId())
                            .build())
                    .toList();

            List<UserExportDto> userDtos = users.stream()
                    .map(u -> UserExportDto.builder()
                            .id(u.getId())
                            .username(u.getUsername())
                            .password(u.getPassword())
                            .role(u.getRole())
                            .build())
                    .toList();

            System.out.println("[EXPORT] Sérialisation des DTOs...");
            ExportDto exportData = ExportDto.builder()
                    .categories(categoryDtos)
                    .trips(tripDtos)
                    .expenses(expenseDtos)
                    .plannerEvents(plannerEventDtos)
                    .users(userDtos)
                    .build();

            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(out, exportData);

            out.flush();
            System.out.println("[EXPORT] Export terminé avec succès !");
        } catch (Exception e) {
            System.err.println("[EXPORT] Erreur lors de l'export : " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // ----------------------------------------------------------------
    // Import: remplace toutes les données par celles du JSON fourni
    // ----------------------------------------------------------------
    public void importFromJson(InputStream in) throws IOException {
        try {
            ExportDto data = objectMapper.readValue(in, ExportDto.class);
            System.out.println("[IMPORT] Données JSON lues avec succès");

            List<CategoryExportDto> categories = data.getCategories() != null ? data.getCategories() : List.of();
            List<TripExportDto> trips = data.getTrips() != null ? data.getTrips() : List.of();
            List<ExpenseExportDto> expenses = data.getExpenses() != null ? data.getExpenses() : List.of();
            List<PlannerEventExportDto> plannerEvents = data.getPlannerEvents() != null ? data.getPlannerEvents() : List.of();
            List<UserExportDto> users = data.getUsers() != null ? data.getUsers() : List.of();

            // Suppression UNIQUEMENT des dépenses, événements planner et voyages (pas les catégories)
            System.out.println("[IMPORT] Suppression des données existantes...");
            plannerEventRepository.deleteAll();
            expenseRepository.deleteAll();
            tripRepository.deleteAll();
            System.out.println("[IMPORT] Nettoyage terminé");

            // ===== ÉTAPE 1 : Importer les catégories EN PREMIER (upsert: créer si absent) =====
            System.out.println("[IMPORT] Importation des catégories...");
            Map<String, CategoryEntity> categoryByName = new HashMap<>();

            if (!categories.isEmpty()) {
                for (CategoryExportDto catDto : categories) {
                    try {
                        // Chercher d'abord si la catégorie existe
                        CategoryEntity existing = categoryService.findByName(catDto.getName());
                        categoryByName.put(catDto.getName(), existing);
                        System.out.println("[IMPORT] Catégorie existante (réutilisée) : " + catDto.getName());
                    } catch (EntityNotFoundException e) {
                        // Créer seulement si elle n'existe pas
                        try {
                            CategoryEntity newCategory = CategoryEntity.builder()
                                    .name(catDto.getName())
                                    .icon(catDto.getIcon())
                                    .color(catDto.getColor())
                                    .editable(catDto.getEditable() != null ? catDto.getEditable() : true)
                                    .build();
                            CategoryEntity saved = categoryRepository.save(newCategory);
                            categoryRepository.flush();
                            categoryByName.put(catDto.getName(), saved);
                            System.out.println("[IMPORT] Catégorie créée : " + catDto.getName());
                        } catch (Exception ex) {
                            System.err.println("[IMPORT] Erreur lors de la création de la catégorie " + catDto.getName() + " : " + ex.getMessage());
                            ex.printStackTrace();
                            throw new IOException("Erreur lors de la création de la catégorie : " + ex.getMessage(), ex);
                        }
                    }
                }
            }
            System.out.println("[IMPORT] Catégories importées/réutilisées : " + categoryByName.size());

            // ===== ÉTAPE 2 : Créer les voyages =====
            System.out.println("[IMPORT] Importation des voyages...");
            Map<Long, Trip> tripById = new HashMap<>();
            try {
                for (TripExportDto dto : trips) {
                    try {
                        Trip saved = tripRepository.save(Trip.builder()
                                .name(dto.getName()).startDate(dto.getStartDate())
                                .endDate(dto.getEndDate()).budget(dto.getBudget())
                                .imageUrl(dto.getImageUrl())
                                .dailyBudgetThreshold(dto.getDailyBudgetThreshold() != null ? dto.getDailyBudgetThreshold() : java.math.BigDecimal.ZERO)
                                .dailyExpenseBudget(dto.getDailyExpenseBudget() != null ? dto.getDailyExpenseBudget() : java.math.BigDecimal.ZERO)
                                .latitude(dto.getLatitude()).longitude(dto.getLongitude())
                                .build());
                        tripById.put(dto.getId(), saved);
                    } catch (Exception ex) {
                        System.err.println("[IMPORT] Erreur lors de la création du voyage " + dto.getName() + " : " + ex.getMessage());
                        ex.printStackTrace();
                        throw new IOException("Erreur lors de l'import voyage : " + ex.getMessage(), ex);
                    }
                }
                tripRepository.flush();
                System.out.println("[IMPORT] Voyages importés : " + tripById.size());
            } catch (Exception ex) {
                System.err.println("[IMPORT] Erreur générale lors de l'importation des voyages : " + ex.getMessage());
                ex.printStackTrace();
                throw new IOException("Erreur lors de l'importation des voyages : " + ex.getMessage(), ex);
            }

            // ===== ÉTAPE 3 : Créer les dépenses avec les catégories importées =====
            System.out.println("[IMPORT] Importation des dépenses...");
            List<Expense> toSave = new ArrayList<>();
            int dépensesOk = 0;
            int dépensesErreur = 0;

            try {
                for (ExpenseExportDto dto : expenses) {
                    Trip trip = tripById.get(dto.getTripId());
                    if (trip == null) {
                        System.err.println("[IMPORT] Voyage non trouvé pour l'expense (tripId: " + dto.getTripId() + ")");
                        dépensesErreur++;
                        continue;
                    }

                    try {
                        CategoryEntity category = categoryByName.get(dto.getCategoryName());
                        if (category == null) {
                            // Fallback : chercher dans les catégories existantes
                            try {
                                category = categoryService.findByName(dto.getCategoryName());
                            } catch (EntityNotFoundException e2) {
                                System.err.println("[IMPORT] Catégorie non trouvée : " + dto.getCategoryName());
                                dépensesErreur++;
                                continue;
                            }
                        }

                        toSave.add(Expense.builder()
                                .amount(dto.getAmount()).date(dto.getDate())
                                .category(category).label(dto.getLabel())
                                .numberOfDays(dto.getNumberOfDays() != null ? dto.getNumberOfDays() : 1)
                                .isPaid(dto.getIsPaid() != null ? dto.getIsPaid() : false)
                                .trip(trip).build());
                        dépensesOk++;
                    } catch (Exception ex) {
                        System.err.println("[IMPORT] Erreur lors de la création d'une dépense : " + ex.getMessage());
                        dépensesErreur++;
                    }
                }

                System.out.println("[IMPORT] Sauvegarde de " + toSave.size() + " dépenses...");
                expenseRepository.saveAll(toSave);
                expenseRepository.flush();
                System.out.println("[IMPORT] Dépenses en mémoire (OK: " + dépensesOk + ", Erreurs: " + dépensesErreur + ")");
            } catch (Exception ex) {
                System.err.println("[IMPORT] Erreur lors de l'importation des dépenses : " + ex.getMessage());
                ex.printStackTrace();
                throw new IOException("Erreur lors de l'importation des dépenses : " + ex.getMessage(), ex);
            }

            // ===== ÉTAPE 4 : Créer les événements planner =====
            System.out.println("[IMPORT] Importation des événements planner...");
            if (!plannerEvents.isEmpty()) {
                List<PlannerEvent> plannerEventsToSave = new ArrayList<>();
                int plannerOk = 0;
                int plannerErreur = 0;
                for (PlannerEventExportDto dto : plannerEvents) {
                    Trip trip = tripById.get(dto.getTripId());
                    if (trip == null) {
                        System.err.println("[IMPORT] Voyage non trouvé pour l'événement planner (tripId: " + dto.getTripId() + ")");
                        plannerErreur++;
                        continue;
                    }
                    plannerEventsToSave.add(PlannerEvent.builder()
                            .name(dto.getName())
                            .eventDateTime(dto.getEventDateTime())
                            .location(dto.getLocation())
                            .comment(dto.getComment())
                            .trip(trip)
                            .build());
                    plannerOk++;
                }
                plannerEventRepository.saveAll(plannerEventsToSave);
                plannerEventRepository.flush();
                System.out.println("[IMPORT] Événements planner importés (OK: " + plannerOk + ", Erreurs: " + plannerErreur + ")");
            } else {
                System.out.println("[IMPORT] Aucun événement planner à importer.");
            }

            // ===== ÉTAPE 5 : Importer les utilisateurs =====
            System.out.println("[IMPORT] Importation des utilisateurs...");
            if (!users.isEmpty()) {
                // Supprimer tous les utilisateurs existants et les remplacer
                appUserRepository.deleteAll();
                appUserRepository.flush();
                int usersOk = 0;
                for (UserExportDto dto : users) {
                    try {
                        AppUser user = AppUser.builder()
                                .username(dto.getUsername())
                                .password(dto.getPassword()) // BCrypt hash restauré tel quel
                                .role(dto.getRole())
                                .build();
                        appUserRepository.save(user);
                        usersOk++;
                        System.out.println("[IMPORT] Utilisateur importé : " + dto.getUsername() + " (" + dto.getRole() + ")");
                    } catch (Exception ex) {
                        System.err.println("[IMPORT] Erreur lors de l'import de l'utilisateur " + dto.getUsername() + " : " + ex.getMessage());
                    }
                }
                appUserRepository.flush();
                System.out.println("[IMPORT] Utilisateurs importés : " + usersOk);
            } else {
                System.out.println("[IMPORT] Aucun utilisateur à importer.");
            }

            System.out.println("[IMPORT] Import terminé avec succès !");
        } catch (IOException e) {
            System.err.println("[IMPORT] Erreur IOException : " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("[IMPORT] Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Erreur critique lors de l'import : " + e.getMessage(), e);
        }
    }
}