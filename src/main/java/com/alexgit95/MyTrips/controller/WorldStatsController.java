package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.dto.CountryStatsDto;
import com.alexgit95.MyTrips.dto.TripMarkerDto;
import com.alexgit95.MyTrips.service.WorldStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/trips/world")
@RequiredArgsConstructor
public class WorldStatsController {

    private static final Logger log = LoggerFactory.getLogger(WorldStatsController.class);

    private final WorldStatsService worldStatsService;

    @GetMapping
    public String worldStats(Model model) {
        Map<String, List<CountryStatsDto>> statsByContinent = worldStatsService.computeStats();
        int totalCountries = statsByContinent.values().stream().mapToInt(List::size).sum();
        int totalContinents = statsByContinent.size();

        // Get map markers for trips with their stages/locations
        List<TripMarkerDto> markers = worldStatsService.getMapMarkers();
        log.info("Generated {} markers for world map", markers.size());
        if (!markers.isEmpty()) {
            markers.forEach(m -> log.debug("Marker: {} ({},{})", m.getTripName(), m.getLatitude(), m.getLongitude()));
        }
        
        // Convert markers to JSON for Thymeleaf
        ObjectMapper objectMapper = new ObjectMapper();
        String markersJson;
        try {
            markersJson = objectMapper.writeValueAsString(markers);
            log.info("Markers JSON length: {} chars", markersJson.length());
            if (markersJson.length() < 500) {
                log.debug("Markers JSON: {}", markersJson);
            }
        } catch (Exception e) {
            log.error("Error converting markers to JSON", e);
            markersJson = "[]";
        }

        model.addAttribute("statsByContinent", statsByContinent);
        model.addAttribute("totalCountries", totalCountries);
        model.addAttribute("totalContinents", totalContinents);
        model.addAttribute("markersJson", markersJson);
        return "trips/worldstats";
    }
}
