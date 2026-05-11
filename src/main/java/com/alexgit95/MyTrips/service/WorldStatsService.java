package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.CountryStatsDto;
import com.alexgit95.MyTrips.dto.TripMarkerDto;
import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Computes world statistics (visited countries per continent) by:
 * 1. Trip GPS coordinates (latitude / longitude) → GeoCountryResolver
 * 2. Trip.country field (explicit text) → LocationParserService  [if no GPS]
 * 3. PlannerEvent.location (free-text address) → LocationParserService
 *
 * The trip NAME is never used for country detection.
 */
@Service
@RequiredArgsConstructor
public class WorldStatsService {

    private static final Logger log = LoggerFactory.getLogger(WorldStatsService.class);

    private final TripRepository tripRepository;
    private final PlannerEventService plannerEventService;
    private final LocationParserService locationParser;
    private final GeoCountryResolver geoResolver;

    @Transactional(readOnly = true)
    public Map<String, List<CountryStatsDto>> computeStats() {

        Map<String, CountryStatsDto> byIso = new LinkedHashMap<>();

        List<Trip> trips = tripRepository.findAllByOrderByStartDateDesc();

        for (Trip trip : trips) {
            String tripLabel = "Voyage : " + trip.getName();

            // ---- 1. GPS coordinates of the trip ----
            if (trip.getLatitude() != null && trip.getLongitude() != null) {
                String isoCode = geoResolver.resolve(trip.getLatitude(), trip.getLongitude());
                if (isoCode != null) {
                    LocationParserService.LocationInfo info = locationParser.lookupByIso(isoCode);
                    if (info != null) {
                        String subdivision = geoResolver.resolveSubdivision(
                                isoCode, trip.getLatitude(), trip.getLongitude());
                        addCountry(byIso, info, subdivision, tripLabel);
                    }
                }
            }
            // ---- 2. Explicit Trip.country field (fallback when no GPS) ----
            else if (trip.getCountry() != null && !trip.getCountry().isBlank()) {
                LocationParserService.LocationInfo info = locationParser.parse(trip.getCountry());
                if (info != null) {
                    addCountry(byIso, info, info.subdivision(), tripLabel);
                }
            }

            // ---- 3. Planner events (text location field) ----
            List<PlannerEvent> events = plannerEventService.findByTrip(trip.getId());
            for (PlannerEvent event : events) {
                if (event.getLocation() != null && !event.getLocation().isBlank()) {
                    String eventLabel = "Étape : " + event.getName() + " (" + trip.getName() + ")";
                    LocationParserService.LocationInfo info = locationParser.parse(event.getLocation());
                    if (info != null) {
                        addCountry(byIso, info, info.subdivision(), eventLabel);
                    }
                }
            }
        }

        // ---- Group by continent with preferred order ----
        List<String> continentOrder = List.of(
                "Europe", "Amérique du Nord", "Amérique du Sud",
                "Afrique", "Asie", "Océanie", "Antarctique"
        );

        Map<String, List<CountryStatsDto>> result = new LinkedHashMap<>();
        for (String continent : continentOrder) {
            List<CountryStatsDto> list = byIso.values().stream()
                    .filter(c -> continent.equals(c.getContinent()))
                    .sorted(Comparator.comparing(CountryStatsDto::getCountryFr))
                    .toList();
            if (!list.isEmpty()) {
                result.put(continent, list);
            }
        }
        // Any remaining continent not in preferred order
        for (CountryStatsDto dto : byIso.values()) {
            if (!result.containsKey(dto.getContinent())) {
                result.computeIfAbsent(dto.getContinent(), k -> new ArrayList<>()).add(dto);
            }
        }

        return result;
    }

    private void addCountry(Map<String, CountryStatsDto> byIso,
                            LocationParserService.LocationInfo info,
                            String subdivision,
                            String sourceLabel) {
        CountryStatsDto dto = byIso.computeIfAbsent(info.isoCode(), iso ->
                new CountryStatsDto(iso, info.countryFr(), info.flag(), info.continent()));

        if (!dto.getSources().contains(sourceLabel)) {
            dto.getSources().add(sourceLabel);
        }
        if (subdivision != null && !dto.getSubdivisions().contains(subdivision)) {
            dto.getSubdivisions().add(subdivision);
        }
    }

    @Transactional(readOnly = true)
    public int countDistinctCountries() {
        return computeStats().values().stream().mapToInt(List::size).sum();
    }

    @Transactional(readOnly = true)
    public int countDistinctContinents() {
        return computeStats().size();
    }

    /**
     * Retrieves all markers for the world map.
     * Each trip can have:
     * 1. A main marker at trip GPS coordinates (if available)
     * 2. Or a marker at country center (if country is specified but no GPS)
     * 3. Markers for each planner event with location and coordinates
     *
     * Each trip is assigned a unique color to distinguish markers.
     */
    @Transactional(readOnly = true)
    public List<TripMarkerDto> getMapMarkers() {
        log.info("=== getMapMarkers() started ===");
        List<TripMarkerDto> markers = new ArrayList<>();
        List<Trip> trips = tripRepository.findAllByOrderByStartDateDesc();
        log.info("Found {} trips", trips.size());

        // Predefined colors for trips (to distinguish them)
        String[] colors = {
                "#FF6B6B", "#4ECDC4", "#FFE66D", "#95E1D3", "#C7CEEA",
                "#B19CD9", "#FF8B94", "#A8E6CF", "#FFD3B6", "#FFAAA5",
                "#FF8C42", "#2E86C1", "#A23B72", "#F18F01", "#C73E1D"
        };

        for (int tripIndex = 0; tripIndex < trips.size(); tripIndex++) {
            Trip trip = trips.get(tripIndex);
            String tripColor = colors[tripIndex % colors.length];
            log.info("Processing trip #{}: {} (id={})", tripIndex, trip.getName(), trip.getId());

            // Try to get planner events with locations
            List<PlannerEvent> events = plannerEventService.findByTrip(trip.getId());
            log.info("  - Found {} planner events for this trip", events.size());
            
            boolean hasEventMarker = false;

            // Add markers for each planner event with location
            for (PlannerEvent event : events) {
                log.info("    - Event: {} (location='{}')", event.getName(), event.getLocation());
                
                if (event.getLocation() != null && !event.getLocation().isBlank()) {
                    double[] coords = null;

                    // Never geocode during map rendering: only reuse persisted coordinates.
                    if (event.getLatitude() != null && event.getLongitude() != null) {
                        coords = new double[]{event.getLatitude(), event.getLongitude()};
                        log.info("      - Using persisted event coordinates: lat={}, lng={}", coords[0], coords[1]);
                    }
                    
                    if (coords != null && coords.length >= 2) {
                        log.info("      - Geocoded to: lat={}, lng={}", coords[0], coords[1]);
                        
                        TripMarkerDto marker = new TripMarkerDto();
                        marker.setTripId(trip.getId());
                        marker.setTripName(trip.getName());
                        marker.setTripImageUrl(trip.getImageUrl());
                        marker.setColor(tripColor);
                        marker.setLatitude(coords[0]);
                        marker.setLongitude(coords[1]);
                        marker.setTitle(event.getName());
                        marker.setLocation(event.getLocation());
                        marker.setMarkerType("event");
                        marker.setEventName(event.getName());
                        markers.add(marker);
                        log.info("      ✓ Added event marker with exact coordinates");
                        hasEventMarker = true;
                    } else {
                        log.warn("      - Failed to geocode address, will use country center as fallback");
                        
                        // Fallback: try to parse country and use country center
                        LocationParserService.LocationInfo info = locationParser.parse(event.getLocation());
                        if (info != null) {
                            log.info("      - Fallback: parsed to country: {} ({})", info.isoCode(), info.countryFr());
                            
                            double[] centerCoords = GeoCountryResolver.getCountryCenter(info.isoCode());
                            if (centerCoords != null && centerCoords.length >= 2) {
                                log.info("      - Using country center: lat={}, lng={}", centerCoords[0], centerCoords[1]);
                                
                                TripMarkerDto marker = new TripMarkerDto();
                                marker.setTripId(trip.getId());
                                marker.setTripName(trip.getName());
                                marker.setTripImageUrl(trip.getImageUrl());
                                marker.setColor(tripColor);
                                marker.setLatitude(centerCoords[0]);
                                marker.setLongitude(centerCoords[1]);
                                marker.setTitle(event.getName());
                                marker.setLocation(event.getLocation());
                                marker.setMarkerType("event");
                                marker.setEventName(event.getName());
                                markers.add(marker);
                                log.info("      ✓ Added event marker with country center (fallback)");
                                hasEventMarker = true;
                            }
                        }
                    }
                } else {
                    log.debug("      - Event has no location (null or blank)");
                }
            }

            // Add main trip marker only if no event markers were added
            if (!hasEventMarker) {
                log.info("  - No event markers added, creating trip marker");
                
                if (trip.getLatitude() != null && trip.getLongitude() != null) {
                    log.info("    - Using trip GPS: lat={}, lng={}", trip.getLatitude(), trip.getLongitude());
                    
                    // Use trip GPS coordinates
                    TripMarkerDto marker = new TripMarkerDto();
                    marker.setTripId(trip.getId());
                    marker.setTripName(trip.getName());
                    marker.setTripImageUrl(trip.getImageUrl());
                    marker.setColor(tripColor);
                    marker.setLatitude(trip.getLatitude());
                    marker.setLongitude(trip.getLongitude());
                    marker.setTitle(trip.getName());
                    marker.setMarkerType("trip");
                    markers.add(marker);
                    log.info("    ✓ Added trip marker (GPS)");
                } else if (trip.getCountry() != null && !trip.getCountry().isBlank()) {
                    log.info("    - Using trip country: {}", trip.getCountry());
                    
                    // Use country center coordinates
                    LocationParserService.LocationInfo info = locationParser.parse(trip.getCountry());
                    if (info != null) {
                        log.info("      - Parsed to: {}", info.isoCode());
                        
                        double[] coords = GeoCountryResolver.getCountryCenter(info.isoCode());
                        if (coords != null && coords.length >= 2) {
                            log.info("      - Got country center: lat={}, lng={}", coords[0], coords[1]);
                            
                            TripMarkerDto marker = new TripMarkerDto();
                            marker.setTripId(trip.getId());
                            marker.setTripName(trip.getName());
                            marker.setTripImageUrl(trip.getImageUrl());
                            marker.setColor(tripColor);
                            marker.setLatitude(coords[0]);
                            marker.setLongitude(coords[1]);
                            marker.setTitle(trip.getName());
                            marker.setLocation(trip.getCountry());
                            marker.setMarkerType("trip");
                            markers.add(marker);
                            log.info("      ✓ Added trip marker (country)");
                        } else {
                            log.warn("      - No country center found");
                        }
                    } else {
                        log.warn("      - Failed to parse country");
                    }
                } else {
                    log.warn("    - Trip has no GPS and no country field");
                }
            }
        }

        log.info("=== getMapMarkers() finished: {} markers created ===", markers.size());
        return markers;
    }
}
