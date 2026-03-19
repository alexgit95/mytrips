package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.service.PlannerEventService;
import com.alexgit95.MyTrips.service.ReverseGeocodingService;
import com.alexgit95.MyTrips.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/trips/{tripId}/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final TripService tripService;
    private final PlannerEventService plannerEventService;
    private final ReverseGeocodingService reverseGeocodingService;

    // ---------------------------
    // Affichage du planner
    // ---------------------------
    @GetMapping
    public String show(@PathVariable Long tripId, Model model) {
        Trip trip = tripService.findById(tripId);
        Map<LocalDate, java.util.List<PlannerEvent>> eventsByDay =
                plannerEventService.groupByDay(tripId);

        model.addAttribute("trip", trip);
        model.addAttribute("eventsByDay", eventsByDay);
        model.addAttribute("newEvent", new PlannerEvent());
        return "trips/planner";
    }

    // ---------------------------
    // Création d'un événement
    // ---------------------------
    @PostMapping("/events")
    public String create(@PathVariable Long tripId,
                         @Valid @ModelAttribute("newEvent") PlannerEvent event,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            Trip trip = tripService.findById(tripId);
            model.addAttribute("trip", trip);
            model.addAttribute("eventsByDay", plannerEventService.groupByDay(tripId));
            return "trips/planner";
        }
        plannerEventService.create(tripId, event);
        ra.addFlashAttribute("success", "Événement ajouté !");
        return "redirect:/trips/" + tripId + "/planner";
    }

    // ---------------------------
    // Formulaire d'édition (GET)
    // ---------------------------
    @GetMapping("/events/{eventId}/edit")
    public String editForm(@PathVariable Long tripId,
                           @PathVariable Long eventId,
                           Model model) {
        Trip trip = tripService.findById(tripId);
        PlannerEvent event = plannerEventService.findById(eventId);
        Map<LocalDate, java.util.List<PlannerEvent>> eventsByDay =
                plannerEventService.groupByDay(tripId);

        model.addAttribute("trip", trip);
        model.addAttribute("eventsByDay", eventsByDay);
        model.addAttribute("newEvent", new PlannerEvent());
        model.addAttribute("editEvent", event);
        return "trips/planner";
    }

    // ---------------------------
    // Sauvegarde modification
    // ---------------------------
    @PostMapping("/events/{eventId}")
    public String update(@PathVariable Long tripId,
                         @PathVariable Long eventId,
                         @Valid @ModelAttribute("editEvent") PlannerEvent updated,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            Trip trip = tripService.findById(tripId);
            model.addAttribute("trip", trip);
            model.addAttribute("eventsByDay", plannerEventService.groupByDay(tripId));
            model.addAttribute("newEvent", new PlannerEvent());
            return "trips/planner";
        }
        plannerEventService.update(eventId, updated);
        ra.addFlashAttribute("success", "Événement mis à jour !");
        return "redirect:/trips/" + tripId + "/planner";
    }

    // ---------------------------
    // Suppression
    // ---------------------------
    @PostMapping("/events/{eventId}/delete")
    public String delete(@PathVariable Long tripId,
                         @PathVariable Long eventId,
                         RedirectAttributes ra) {
        plannerEventService.delete(eventId);
        ra.addFlashAttribute("success", "Événement supprimé.");
        return "redirect:/trips/" + tripId + "/planner";
    }

    // ---------------------------
    // Suggestion adresse + nom POI (pour le bouton "Ici et maintenant")
    // ---------------------------
    @GetMapping("/geocode")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> geocode(
            @PathVariable Long tripId,
            @RequestParam Double lat,
            @RequestParam Double lon) {

        Map<String, Object> result = new HashMap<>();
        ReverseGeocodingService.ReverseGeocodeResult geo =
                reverseGeocodingService.reverseGeocodeWithPoi(lat, lon);

        if (geo != null) {
            result.put("address",       geo.address  != null ? geo.address  : "");
            result.put("suggestedName", geo.poiName  != null ? geo.poiName  : "");
        } else {
            // Géocodage désactivé ou erreur : renvoyer les coordonnées comme adresse
            result.put("address",       String.format("%.6f, %.6f", lat, lon));
            result.put("suggestedName", "");
        }
        return ResponseEntity.ok(result);
    }

    // ---------------------------
    // Créer un événement "ici et maintenant"
    // ---------------------------
    @PostMapping("/events/here-and-now")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createHereAndNow(
            @PathVariable Long tripId,
            @RequestParam String name,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String prefetchedLocation) {

        Trip trip = tripService.findById(tripId);
        Map<String, Object> response = new HashMap<>();

        // Vérifier que le voyage est en cours
        if (!trip.isOngoing()) {
            response.put("success", false);
            response.put("message", "Ce voyage n'est pas en cours");
            return ResponseEntity.badRequest().body(response);
        }

        // Valider le nom
        if (name == null || name.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Le nom de l'événement est obligatoire");
            return ResponseEntity.badRequest().body(response);
        }

        // Créer l'événement
        PlannerEvent event = new PlannerEvent();
        event.setName(name.trim());
        event.setEventDateTime(LocalDateTime.now());

        // Déterminer la localisation
        String location = null;
        if (prefetchedLocation != null && !prefetchedLocation.isBlank()) {
            // Adresse déjà résolue côté client via /geocode (évite un double appel Nominatim)
            location = prefetchedLocation.trim();
        } else if (latitude != null && longitude != null) {
            // Fallback : résoudre via géocodage inverse
            location = reverseGeocodingService.reverseGeocode(latitude, longitude);
            if (location == null) {
                location = String.format("%.6f, %.6f", latitude, longitude);
            }
        }
        
        event.setLocation(location);
        plannerEventService.create(tripId, event);

        response.put("success", true);
        response.put("message", "Événement créé avec succès !");
        response.put("eventId", event.getId());
        return ResponseEntity.ok(response);
    }
}
