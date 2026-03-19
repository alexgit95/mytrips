package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.CountryStatsDto;
import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import com.alexgit95.MyTrips.repository.TripRepository;
import lombok.RequiredArgsConstructor;
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

    private final TripRepository tripRepository;
    private final PlannerEventRepository plannerEventRepository;
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
            List<PlannerEvent> events =
                    plannerEventRepository.findByTripIdOrderByEventDateTimeAsc(trip.getId());
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
}
