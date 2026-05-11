package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlannerEventService {

    private final PlannerEventRepository plannerEventRepository;
    private final TripService tripService;

    @Transactional(readOnly = true)
    public List<PlannerEvent> findByTrip(Long tripId) {
        return plannerEventRepository.findByTripIdOrderByEventDateTimeAsc(tripId);
    }

    @Transactional(readOnly = true)
    public PlannerEvent findById(Long id) {
        return plannerEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Événement introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, List<PlannerEvent>> groupByDay(Long tripId) {
        return findByTrip(tripId).stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEventDateTime().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));
    }

    @Transactional(readOnly = true)
    public long countWithCoordinates(Long tripId) {
        return plannerEventRepository.countByTripIdWithCoordinates(tripId);
    }

    @Transactional
    public PlannerEvent save(PlannerEvent event) {
        event.setComment(normalizeComment(event.getComment()));
        return plannerEventRepository.save(event);
    }

    @Transactional
    public PlannerEvent create(Long tripId, PlannerEvent event) {
        Trip trip = tripService.findById(tripId);
        event.setTrip(trip);
        if (event.getLocation() == null || event.getLocation().isBlank()) {
            event.setLatitude(null);
            event.setLongitude(null);
        }
        event.setComment(normalizeComment(event.getComment()));
        return plannerEventRepository.save(event);
    }

    @Transactional
    public PlannerEvent update(Long eventId, PlannerEvent updated) {
        PlannerEvent existing = findById(eventId);
        boolean locationChanged = !Objects.equals(existing.getLocation(), updated.getLocation());
        existing.setName(updated.getName());
        existing.setEventDateTime(updated.getEventDateTime());
        existing.setLocation(updated.getLocation());
        if (locationChanged) {
            existing.setLatitude(null);
            existing.setLongitude(null);
        }
        existing.setComment(normalizeComment(updated.getComment()));
        return plannerEventRepository.save(existing);
    }

    @Transactional
    public void delete(Long eventId) {
        plannerEventRepository.deleteById(eventId);
    }

    static String normalizeComment(String comment) {
        if (comment == null || "undefined".equals(comment)) {
            return "";
        }
        return comment;
    }
}
