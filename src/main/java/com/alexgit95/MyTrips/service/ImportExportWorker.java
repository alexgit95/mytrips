package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.AccommodationExportDto;
import com.alexgit95.MyTrips.dto.CategoryExportDto;
import com.alexgit95.MyTrips.dto.ExportDto;
import com.alexgit95.MyTrips.dto.ExpenseExportDto;
import com.alexgit95.MyTrips.dto.PlannerEventExportDto;
import com.alexgit95.MyTrips.dto.TripExportDto;
import com.alexgit95.MyTrips.dto.UserExportDto;
import com.alexgit95.MyTrips.model.Accommodation;
import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.AppUser;
import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.AccommodationRepository;
import com.alexgit95.MyTrips.repository.AppUserRepository;
import com.alexgit95.MyTrips.repository.CategoryRepository;
import com.alexgit95.MyTrips.repository.ExpenseRepository;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import com.alexgit95.MyTrips.repository.TripRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ImportExportWorker.class);

    private final CategoryRepository categoryRepository;
    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;
    private final PlannerEventRepository plannerEventRepository;
    private final AppUserRepository appUserRepository;
    private final AccommodationRepository accommodationRepository;
    private final CategoryService categoryService;
    private final AppSettingsService appSettingsService;
    private final ObjectMapper objectMapper;

    // ----------------------------------------------------------------
    // Export: serialise toutes les données en JSON vers l'OutputStream
    // ----------------------------------------------------------------
    public void exportToJson(OutputStream out) throws IOException {
        try {
            log.info("[EXPORT] Démarrage de l'export JSON...");

            // Exporter uniquement les catégories custom (editable=true)
            List<CategoryEntity> customCategories = categoryRepository.findAll()
                    .stream()
                    .filter(c -> c.getEditable() != null && c.getEditable())
                    .toList();

            List<Trip> trips = tripRepository.findAll();
            List<Expense> expenses = expenseRepository.findAll();
            List<PlannerEvent> plannerEvents = plannerEventRepository.findAll();
            List<AppUser> users = appUserRepository.findAll();
            List<Accommodation> accommodations = accommodationRepository.findAll();
            log.info("[EXPORT] Catégories custom trouvées : {}", customCategories.size());
            log.info("[EXPORT] Voyages trouvés : {}", trips.size());
            log.info("[EXPORT] Dépenses trouvées : {}", expenses.size());
            log.info("[EXPORT] Événements planner trouvés : {}", plannerEvents.size());
            log.info("[EXPORT] Utilisateurs trouvés : {}", users.size());
            log.info("[EXPORT] Logements trouvés : {}", accommodations.size());

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
                        .latitude(pe.getLatitude())
                        .longitude(pe.getLongitude())
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

            List<AccommodationExportDto> accommodationDtos = accommodations.stream()
                    .map(a -> AccommodationExportDto.builder()
                            .id(a.getId())
                            .name(a.getName())
                            .address(a.getAddress())
                            .arrivalDate(a.getArrivalDate())
                            .departureDate(a.getDepartureDate())
                            .latitude(a.getLatitude())
                            .longitude(a.getLongitude())
                            .comment(a.getComment())
                            .tripId(a.getTrip().getId())
                            .build())
                    .toList();

            log.info("[EXPORT] Sérialisation des DTOs...");
            ExportDto exportData = ExportDto.builder()
                    .categories(categoryDtos)
                    .trips(tripDtos)
                    .expenses(expenseDtos)
                    .plannerEvents(plannerEventDtos)
                    .users(userDtos)
                    .accommodations(accommodationDtos)
                    .homeCountry(appSettingsService.getHomeCountry())
                    .build();

            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(out, exportData);

            out.flush();
            log.info("[EXPORT] Export terminé avec succès !");
        } catch (Exception e) {
            log.error("[EXPORT] Erreur lors de l'export", e);
            throw e;
        }
    }

    // ----------------------------------------------------------------
    // Import: remplace toutes les données par celles du JSON fourni
    // ----------------------------------------------------------------
    public void importFromJson(InputStream in) throws IOException {
        try {
            ExportDto data = objectMapper.readValue(in, ExportDto.class);
            log.info("[IMPORT] Données JSON lues avec succès");

            List<CategoryExportDto> categories = data.getCategories() != null ? data.getCategories() : List.of();
            List<TripExportDto> trips = data.getTrips() != null ? data.getTrips() : List.of();
            List<ExpenseExportDto> expenses = data.getExpenses() != null ? data.getExpenses() : List.of();
            List<PlannerEventExportDto> plannerEvents = data.getPlannerEvents() != null ? data.getPlannerEvents() : List.of();
            List<UserExportDto> users = data.getUsers() != null ? data.getUsers() : List.of();
            List<AccommodationExportDto> accommodationsDto = data.getAccommodations() != null ? data.getAccommodations() : List.of();

            // Suppression UNIQUEMENT des dépenses, événements planner, logements et voyages (pas les catégories)
            log.info("[IMPORT] Suppression des données existantes...");
            accommodationRepository.deleteAll();
            plannerEventRepository.deleteAll();
            expenseRepository.deleteAll();
            tripRepository.deleteAll();
            log.info("[IMPORT] Nettoyage terminé");

            // ===== ÉTAPE 1 : Importer les catégories EN PREMIER (upsert: créer si absent) =====
            log.info("[IMPORT] Importation des catégories...");
            Map<String, CategoryEntity> categoryByName = new HashMap<>();

            if (!categories.isEmpty()) {
                for (CategoryExportDto catDto : categories) {
                    try {
                        // Chercher d'abord si la catégorie existe
                        CategoryEntity existing = categoryService.findByName(catDto.getName());
                        categoryByName.put(catDto.getName(), existing);
                        log.info("[IMPORT] Catégorie existante (réutilisée) : {}", catDto.getName());
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
                            log.info("[IMPORT] Catégorie créée : {}", catDto.getName());
                        } catch (Exception ex) {
                            log.error("[IMPORT] Erreur lors de la création de la catégorie {}", catDto.getName(), ex);
                            throw new IOException("Erreur lors de la création de la catégorie : " + ex.getMessage(), ex);
                        }
                    }
                }
            }
            log.info("[IMPORT] Catégories importées/réutilisées : {}", categoryByName.size());

            // ===== ÉTAPE 2 : Créer les voyages =====
            log.info("[IMPORT] Importation des voyages...");
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
                        log.error("[IMPORT] Erreur lors de la création du voyage {}", dto.getName(), ex);
                        throw new IOException("Erreur lors de l'import voyage : " + ex.getMessage(), ex);
                    }
                }
                tripRepository.flush();
                log.info("[IMPORT] Voyages importés : {}", tripById.size());
            } catch (Exception ex) {
                log.error("[IMPORT] Erreur générale lors de l'importation des voyages", ex);
                throw new IOException("Erreur lors de l'importation des voyages : " + ex.getMessage(), ex);
            }

            // ===== ÉTAPE 3 : Créer les dépenses avec les catégories importées =====
            log.info("[IMPORT] Importation des dépenses...");
            List<Expense> toSave = new ArrayList<>();
            int dépensesOk = 0;
            int dépensesErreur = 0;

            try {
                for (ExpenseExportDto dto : expenses) {
                    Trip trip = tripById.get(dto.getTripId());
                    if (trip == null) {
                        log.warn("[IMPORT] Voyage non trouvé pour l'expense (tripId: {})", dto.getTripId());
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
                                log.warn("[IMPORT] Catégorie non trouvée : {}", dto.getCategoryName());
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
                        log.error("[IMPORT] Erreur lors de la création d'une dépense", ex);
                        dépensesErreur++;
                    }
                }

                System.out.println("[IMPORT] Sauvegarde de " + toSave.size() + " dépenses...");
                expenseRepository.saveAll(toSave);
                expenseRepository.flush();
                log.info("[IMPORT] Dépenses en mémoire (OK: {}, Erreurs: {})", dépensesOk, dépensesErreur);
            } catch (Exception ex) {
                log.error("[IMPORT] Erreur lors de l'importation des dépenses", ex);
                throw new IOException("Erreur lors de l'importation des dépenses : " + ex.getMessage(), ex);
            }

            // ===== ÉTAPE 4 : Créer les événements planner =====
            log.info("[IMPORT] Importation des événements planner...");
            if (!plannerEvents.isEmpty()) {
                List<PlannerEvent> plannerEventsToSave = new ArrayList<>();
                int plannerOk = 0;
                int plannerErreur = 0;
                for (PlannerEventExportDto dto : plannerEvents) {
                    Trip trip = tripById.get(dto.getTripId());
                    if (trip == null) {
                        log.warn("[IMPORT] Voyage non trouvé pour l'événement planner (tripId: {})", dto.getTripId());
                        plannerErreur++;
                        continue;
                    }
                    plannerEventsToSave.add(PlannerEvent.builder()
                            .name(dto.getName())
                            .eventDateTime(dto.getEventDateTime())
                            .location(dto.getLocation())
                            .latitude(dto.getLatitude())
                            .longitude(dto.getLongitude())
                            .comment(dto.getComment())
                            .trip(trip)
                            .build());
                    plannerOk++;
                }
                plannerEventRepository.saveAll(plannerEventsToSave);
                plannerEventRepository.flush();
                log.info("[IMPORT] Événements planner importés (OK: {}, Erreurs: {})", plannerOk, plannerErreur);
            } else {
                log.info("[IMPORT] Aucun événement planner à importer.");
            }

            // ===== ÉTAPE 5 : Importer les logements =====
            log.info("[IMPORT] Importation des logements...");
            if (!accommodationsDto.isEmpty()) {
                List<Accommodation> accommodationsToSave = new ArrayList<>();
                int accOk = 0;
                int accErreur = 0;
                for (AccommodationExportDto dto : accommodationsDto) {
                    Trip trip = tripById.get(dto.getTripId());
                    if (trip == null) {
                        log.warn("[IMPORT] Voyage non trouvé pour le logement (tripId: {})", dto.getTripId());
                        accErreur++;
                        continue;
                    }
                    accommodationsToSave.add(Accommodation.builder()
                            .name(dto.getName())
                            .address(dto.getAddress())
                            .arrivalDate(dto.getArrivalDate())
                            .departureDate(dto.getDepartureDate())
                            .latitude(dto.getLatitude())
                            .longitude(dto.getLongitude())
                            .comment(dto.getComment())
                            .trip(trip)
                            .build());
                    accOk++;
                }
                accommodationRepository.saveAll(accommodationsToSave);
                accommodationRepository.flush();
                log.info("[IMPORT] Logements importés (OK: {}, Erreurs: {})", accOk, accErreur);
            } else {
                log.info("[IMPORT] Aucun logement à importer.");
            }

            // ===== ÉTAPE 6 : Importer les utilisateurs =====
            log.info("[IMPORT] Importation des utilisateurs...");
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
                        log.info("[IMPORT] Utilisateur importé : {} ({})", dto.getUsername(), dto.getRole());
                    } catch (Exception ex) {
                        log.error("[IMPORT] Erreur lors de l'import de l'utilisateur {}", dto.getUsername(), ex);
                    }
                }
                appUserRepository.flush();
                log.info("[IMPORT] Utilisateurs importés : {}", usersOk);
            } else {
                log.info("[IMPORT] Aucun utilisateur à importer.");
            }

            // ===== ÉTAPE 7 : Restaurer le pays d'origine =====
            if (data.getHomeCountry() != null && !data.getHomeCountry().isBlank()) {
                appSettingsService.setHomeCountry(data.getHomeCountry());
                log.info("[IMPORT] Pays d'origine restauré : {}", data.getHomeCountry());
            }

            log.info("[IMPORT] Import terminé avec succès !");
        } catch (IOException e) {
            log.error("[IMPORT] Erreur IOException", e);
            throw e;
        } catch (Exception e) {
            log.error("[IMPORT] Erreur inattendue", e);
            throw new IOException("Erreur critique lors de l'import : " + e.getMessage(), e);
        }
    }
}