package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import com.alexgit95.MyTrips.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlannerEventService {

    private final PlannerEventRepository plannerEventRepository;
    private final TripRepository tripRepository;

    public List<PlannerEvent> findByTrip(Long tripId) {
        return plannerEventRepository.findByTripIdOrderByEventDateTimeAsc(tripId);
    }

    public PlannerEvent findById(Long id) {
        return plannerEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Événement introuvable : " + id));
    }

    /**
     * Group events by day (LocalDate), preserving chronological order.
     */
    public Map<LocalDate, List<PlannerEvent>> groupByDay(Long tripId) {
        return findByTrip(tripId).stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEventDateTime().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));
    }

    @Transactional
    public PlannerEvent save(PlannerEvent event) {
        return plannerEventRepository.save(event);
    }

    @Transactional
    public PlannerEvent create(Long tripId, PlannerEvent event) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Voyage introuvable : " + tripId));
        event.setTrip(trip);
        return plannerEventRepository.save(event);
    }

    @Transactional
    public PlannerEvent update(Long eventId, PlannerEvent updated) {
        PlannerEvent existing = findById(eventId);
        existing.setName(updated.getName());
        existing.setEventDateTime(updated.getEventDateTime());
        existing.setLocation(updated.getLocation());
        existing.setComment(updated.getComment());
        return plannerEventRepository.save(existing);
    }

    @Transactional
    public void delete(Long eventId) {
        plannerEventRepository.deleteById(eventId);
    }
}
