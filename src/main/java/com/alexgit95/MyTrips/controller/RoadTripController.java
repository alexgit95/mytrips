package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.service.AppSettingsService;
import com.alexgit95.MyTrips.service.GeoCountryResolver;
import com.alexgit95.MyTrips.service.PlannerEventService;
import com.alexgit95.MyTrips.service.TripService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la vue Road Trip d'un voyage.
 * Affiche une carte OpenStreetMap avec l'itinéraire routier entre tous les points du planner,
 * filtrés selon le pays d'origine configuré dans l'administration.
 */
@Controller
@RequestMapping("/trips")
@RequiredArgsConstructor
public class RoadTripController {

    private final TripService           tripService;
    private final PlannerEventService   plannerEventService;
    private final AppSettingsService    appSettingsService;
    private final GeoCountryResolver    geoCountryResolver;
    private final ObjectMapper          objectMapper;

    @GetMapping("/{id}/road-trip")
    public String roadTrip(@PathVariable Long id, Model model) throws Exception {

        Trip trip = tripService.findById(id);

        // Récupérer tous les événements avec coordonnées, ordonnés par date
        List<PlannerEvent> eventsWithCoords = plannerEventService.findByTrip(id).stream()
                .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
                .collect(Collectors.toList());

        // Récupérer le pays d'origine configuré (code ISO ex: "FR")
        String homeCountryIso = appSettingsService.getHomeCountry();

        // Déterminer si le voyage se déroule à l'étranger
        // = au moins un point se situe hors du pays d'origine
        boolean isAbroad = eventsWithCoords.stream()
                .anyMatch(e -> {
                    String iso = geoCountryResolver.resolve(e.getLatitude(), e.getLongitude());
                    return iso != null && !iso.equalsIgnoreCase(homeCountryIso);
                });

        // Si voyage à l'étranger, exclure les points situés dans le pays d'origine
        List<PlannerEvent> filteredEvents;
        if (isAbroad) {
            filteredEvents = eventsWithCoords.stream()
                    .filter(e -> {
                        String iso = geoCountryResolver.resolve(e.getLatitude(), e.getLongitude());
                        // Garder si pays inconnu ou différent du pays d'origine
                        return iso == null || !iso.equalsIgnoreCase(homeCountryIso);
                    })
                    .collect(Collectors.toList());
        } else {
            filteredEvents = new ArrayList<>(eventsWithCoords);
        }

        // Construire la liste de waypoints pour le JS (ordre date)
        List<Map<String, Object>> waypoints = filteredEvents.stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getName());
                    m.put("lat", e.getLatitude());
                    m.put("lng", e.getLongitude());
                    m.put("date", e.getEventDateTime().toLocalDate().toString());
                    m.put("location", e.getLocation() != null ? e.getLocation() : "");
                    return m;
                })
                .collect(Collectors.toList());

        model.addAttribute("trip", trip);
        model.addAttribute("waypointsJson", objectMapper.writeValueAsString(waypoints));
        model.addAttribute("isAbroad", isAbroad);
        model.addAttribute("homeCountry", homeCountryIso);

        return "trips/road-trip";
    }
}
