package com.alexgit95.MyTrips.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.service.AppSettingsService;
import com.alexgit95.MyTrips.service.GeoCountryResolver;
import com.alexgit95.MyTrips.service.PlannerEventService;
import com.alexgit95.MyTrips.service.TripService;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

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

        // Séparer les points selon qu'ils sont dans le pays d'origine ou à l'étranger
        List<PlannerEvent> homeCountryEvents = eventsWithCoords.stream()
                .filter(e -> {
                    String iso = geoCountryResolver.resolve(e.getLatitude(), e.getLongitude());
                    return iso != null && iso.equalsIgnoreCase(homeCountryIso);
                })
                .collect(Collectors.toList());

        boolean isAbroad = homeCountryEvents.size() < eventsWithCoords.size(); // au moins un point étranger

        // Règle de filtrage :
        // - Si le voyage est à l'étranger ET que les points dans le pays d'origine sont <= 4
        //   → exclure ces points (simples escales de départ/retour)
        // - Si le voyage est à l'étranger ET que les points dans le pays d'origine sont > 4
        //   → voyage à cheval sur deux pays, on affiche TOUS les points
        // - Si voyage entièrement dans le pays d'origine → afficher tous les points
        boolean homePointsFiltered = isAbroad && homeCountryEvents.size() <= 4;

        List<PlannerEvent> filteredEvents;
        if (homePointsFiltered) {
            filteredEvents = eventsWithCoords.stream()
                    .filter(e -> {
                        String iso = geoCountryResolver.resolve(e.getLatitude(), e.getLongitude());
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
        model.addAttribute("homePointsFiltered", homePointsFiltered);
        model.addAttribute("homeCountry", homeCountryIso);
        model.addAttribute("homeCountryPointCount", homeCountryEvents.size());

        return "trips/road-trip";
    }
}
