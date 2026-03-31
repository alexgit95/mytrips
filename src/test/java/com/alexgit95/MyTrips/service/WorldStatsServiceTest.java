package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.dto.CountryStatsDto;
import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import com.alexgit95.MyTrips.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldStatsServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private PlannerEventRepository plannerEventRepository;

    @Mock
    private GeoCountryResolver geoResolver;

    private final LocationParserService locationParser = new LocationParserService();

    @Test
    void computeStats_shouldMergeSourcesAndAvoidDuplicates() {
        Trip trip = Trip.builder()
                .id(1L)
                .name("France Trip")
                .startDate(LocalDate.of(2026, 1, 10))
                .endDate(LocalDate.of(2026, 1, 15))
                .latitude(48.8566)
                .longitude(2.3522)
                .country("France")
                .build();

        PlannerEvent event1 = PlannerEvent.builder()
                .name("Eiffel")
                .eventDateTime(LocalDateTime.of(2026, 1, 11, 10, 0))
                .location("Paris, France")
                .trip(trip)
                .build();

        PlannerEvent event2 = PlannerEvent.builder()
                .name("Eiffel")
                .eventDateTime(LocalDateTime.of(2026, 1, 11, 10, 5))
                .location("Paris, France")
                .trip(trip)
                .build();

        when(tripRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(trip));
        when(plannerEventRepository.findByTripIdOrderByEventDateTimeAsc(1L)).thenReturn(List.of(event1, event2));
        when(geoResolver.resolve(48.8566, 2.3522)).thenReturn("FR");
        when(geoResolver.resolveSubdivision("FR", 48.8566, 2.3522)).thenReturn("Paris");

        WorldStatsService service = new WorldStatsService(
                tripRepository,
                plannerEventRepository,
                locationParser,
                geoResolver
        );

        Map<String, List<CountryStatsDto>> stats = service.computeStats();
        CountryStatsDto france = flattenByIso(stats).get("FR");

        assertNotNull(france);
        assertEquals("France", france.getCountryFr());
        assertEquals(1, france.getSubdivisions().size());
        assertEquals("Paris", france.getSubdivisions().get(0));
        assertEquals(2, france.getSources().size());
        assertTrue(france.getSources().stream().anyMatch(s -> s.contains("Voyage")));
        assertTrue(france.getSources().stream().anyMatch(s -> s.contains("Eiffel")));
    }

    @Test
    void computeStats_shouldUseTripCountryWhenGpsIsMissing() {
        Trip trip = Trip.builder()
                .id(2L)
                .name("Texas Trip")
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 8))
                .country("Austin, TX, USA")
                .build();

        when(tripRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(trip));
        when(plannerEventRepository.findByTripIdOrderByEventDateTimeAsc(2L)).thenReturn(List.of());

        WorldStatsService service = new WorldStatsService(
                tripRepository,
                plannerEventRepository,
                locationParser,
                geoResolver
        );

        Map<String, List<CountryStatsDto>> stats = service.computeStats();
        CountryStatsDto usa = flattenByIso(stats).get("US");

        assertNotNull(usa);
        assertEquals("Etats-Unis", usa.getCountryFr().replace("É", "E"));
        assertTrue(usa.getSubdivisions().contains("Texas"));
    }

    private Map<String, CountryStatsDto> flattenByIso(Map<String, List<CountryStatsDto>> stats) {
        Map<String, CountryStatsDto> byIso = new HashMap<>();
        for (List<CountryStatsDto> list : stats.values()) {
            for (CountryStatsDto dto : list) {
                byIso.put(dto.getIsoCode(), dto);
            }
        }
        return byIso;
    }
}
