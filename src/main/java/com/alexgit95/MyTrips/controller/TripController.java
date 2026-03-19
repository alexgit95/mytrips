package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.dto.ChartDataDto;
import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.service.CategoryService;
import com.alexgit95.MyTrips.service.ExpenseService;
import com.alexgit95.MyTrips.service.TripService;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService       tripService;
    private final ExpenseService    expenseService;
    private final CategoryService   categoryService;
    private final ObjectMapper      objectMapper;

    // ---------------------------
    // Liste des voyages
    // ---------------------------
    @GetMapping
    public String list(Model model) {
        List<Trip> trips = tripService.findAll();
        model.addAttribute("trips", trips);
        // Map tripId → montant dépensé
        Map<Long, BigDecimal> spentMap = new HashMap<>();
        trips.forEach(t -> spentMap.put(t.getId(), expenseService.totalByTrip(t.getId())));
        model.addAttribute("spentMap", spentMap);
        // Map tripId -> per-day remaining and threshold flag
        Map<Long, java.math.BigDecimal> perDayMap = new HashMap<>();
        Map<Long, Boolean> belowMap = new HashMap<>();
        Map<Long, Long> remainingDaysMap = new HashMap<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (Trip t : trips) {
            BigDecimal spent = spentMap.getOrDefault(t.getId(), BigDecimal.ZERO);
            BigDecimal remaining = t.getBudget().subtract(spent);
            java.time.LocalDate from = today.isAfter(t.getStartDate()) ? today : t.getStartDate();
            long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(from, t.getEndDate()) + 1;
            if (remainingDays < 1) remainingDays = 1;
            java.math.BigDecimal perDay = remaining.divide(java.math.BigDecimal.valueOf(remainingDays), 2, java.math.RoundingMode.HALF_UP);
            boolean below = false;
            if (t.getDailyBudgetThreshold() != null && t.getDailyBudgetThreshold().compareTo(java.math.BigDecimal.ZERO) > 0) {
                below = perDay.compareTo(t.getDailyBudgetThreshold()) < 0;
            }
            perDayMap.put(t.getId(), perDay);
            belowMap.put(t.getId(), below);
            remainingDaysMap.put(t.getId(), remainingDays);
        }
        model.addAttribute("perDayMap", perDayMap);
        model.addAttribute("belowMap", belowMap);
        model.addAttribute("remainingDaysMap", remainingDaysMap);

        // Serialize geo-coordinates as JSON for Leaflet map
        try {
            List<Map<String, Object>> markers = new java.util.ArrayList<>();
            for (Trip t : trips) {
                if (t.getLatitude() != null && t.getLongitude() != null) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("lat", t.getLatitude());
                    m.put("lng", t.getLongitude());
                    m.put("imageUrl", t.getImageUrl() != null ? t.getImageUrl() : "");
                    markers.add(m);
                }
            }
            model.addAttribute("markersJson", objectMapper.writeValueAsString(markers));
        } catch (Exception e) {
            model.addAttribute("markersJson", "[]");
        }
        return "trips/list";
    }

    // ---------------------------
    // Formulaire de création
    // ---------------------------
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("trip", new Trip());
        model.addAttribute("pageTitle", "Nouveau voyage");
        return "trips/form";
    }

    // ---------------------------
    // Sauvegarde création
    // ---------------------------
    @PostMapping
    public String create(@Valid @ModelAttribute("trip") Trip trip,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Nouveau voyage");
            return "trips/form";
        }
        tripService.save(trip);
        ra.addFlashAttribute("success", "Voyage créé avec succès !");
        return "redirect:/trips";
    }

    // ---------------------------
    // Formulaire d'édition
    // ---------------------------
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("trip", tripService.findById(id));
        model.addAttribute("pageTitle", "Modifier le voyage");
        return "trips/form";
    }

    // ---------------------------
    // Sauvegarde modification
    // ---------------------------
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("trip") Trip trip,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Modifier le voyage");
            return "trips/form";
        }
        // Charger l'entité existante pour préserver la collection `expenses`
        Trip existing = tripService.findById(id);
        existing.setName(trip.getName());
        existing.setStartDate(trip.getStartDate());
        existing.setEndDate(trip.getEndDate());
        existing.setBudget(trip.getBudget());
        existing.setImageUrl(trip.getImageUrl());
        existing.setDailyBudgetThreshold(trip.getDailyBudgetThreshold());
        existing.setDailyExpenseBudget(trip.getDailyExpenseBudget());
        existing.setLatitude(trip.getLatitude());
        existing.setLongitude(trip.getLongitude());
        existing.setCountry(trip.getCountry());

        tripService.save(existing);
        ra.addFlashAttribute("success", "Voyage mis à jour !");
        return "redirect:/trips";
    }

    // ---------------------------
    // Suppression
    // ---------------------------
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        tripService.delete(id);
        ra.addFlashAttribute("success", "Voyage supprimé.");
        return "redirect:/trips";
    }

    // ---------------------------
    // Détail d'un voyage
    // ---------------------------
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) throws Exception {

        Trip trip = tripService.findById(id);
        List<Expense> expenses = expenseService.findByTrip(id);
        BigDecimal total = expenseService.totalByTrip(id);
        BigDecimal remaining = trip.getBudget().subtract(total);

        // Compute remaining days and budget per day remaining
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate from = today.isAfter(trip.getStartDate()) ? today : trip.getStartDate();
        long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(from, trip.getEndDate()) + 1;
        if (remainingDays < 1) remainingDays = 1;
        java.math.BigDecimal perDayRemaining = remaining.compareTo(java.math.BigDecimal.ZERO) >= 0
                ? remaining.divide(java.math.BigDecimal.valueOf(remainingDays), 2, java.math.RoundingMode.HALF_UP)
                : remaining.divide(java.math.BigDecimal.valueOf(remainingDays), 2, java.math.RoundingMode.HALF_UP);

        boolean belowThreshold = false;
        if (trip.getDailyBudgetThreshold() != null && trip.getDailyBudgetThreshold().compareTo(java.math.BigDecimal.ZERO) > 0) {
            belowThreshold = perDayRemaining.compareTo(trip.getDailyBudgetThreshold()) < 0;
        }

        model.addAttribute("remainingDays", remainingDays);
        model.addAttribute("perDayRemaining", perDayRemaining);
        model.addAttribute("belowThreshold", belowThreshold);

        ChartDataDto chartData = expenseService.buildChartData(trip);

        model.addAttribute("trip", trip);
        model.addAttribute("expenses", expenses);
        model.addAttribute("total", total);
        model.addAttribute("remaining", remaining);
        model.addAttribute("categories", categoryService.findAll());

        // Sérialiser les données de graphiques en JSON pour Chart.js
        model.addAttribute("chartLabelsJson",  objectMapper.writeValueAsString(chartData.getLabels()));
        model.addAttribute("chartActualJson",  objectMapper.writeValueAsString(chartData.getActualCumulative()));
        model.addAttribute("chartBudgetJson",  objectMapper.writeValueAsString(chartData.getBudgetLine()));
        model.addAttribute("chartTrendJson",   objectMapper.writeValueAsString(chartData.getTrendLine()));
        model.addAttribute("catLabelsJson",    objectMapper.writeValueAsString(chartData.getCategoryLabels()));
        model.addAttribute("catAmountsJson",   objectMapper.writeValueAsString(chartData.getCategoryAmounts()));

        return "trips/detail";
    }

    // ---------------------------
    // Frise chronologique
    // ---------------------------
    @GetMapping("/timeline")
    public String timeline(Model model) {
        List<Trip> trips = tripService.findAllChronological();
        Map<Long, BigDecimal> spentMap = new HashMap<>();
        trips.forEach(t -> spentMap.put(t.getId(), expenseService.totalByTrip(t.getId())));
        // Group trips by year (insertion order preserved)
        LinkedHashMap<String, List<Trip>> tripsByYear = trips.stream()
                .collect(Collectors.groupingBy(
                        t -> String.valueOf(t.getStartDate().getYear()),
                        LinkedHashMap::new,
                        Collectors.toList()));
        model.addAttribute("tripsByYear", tripsByYear);
        model.addAttribute("spentMap", spentMap);
        return "trips/timeline";
    }
}
