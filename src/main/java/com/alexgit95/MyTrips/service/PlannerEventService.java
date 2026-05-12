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

import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlannerEventService {

    private final PlannerEventRepository plannerEventRepository;
    private final TripService tripService;
    private final ForwardGeocodingService forwardGeocodingService;

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
        // Géocoder l'adresse si elle est renseignée mais sans coordonnées
        geocodeIfNeeded(event);
        event.setComment(normalizeComment(event.getComment()));
        return plannerEventRepository.save(event);
    }

    @Transactional
    public PlannerEvent update(Long eventId, PlannerEvent updated) {
        PlannerEvent existing = findById(eventId);
        existing.setName(updated.getName());
        existing.setEventDateTime(updated.getEventDateTime());
        existing.setLocation(updated.getLocation());
        // Propager les coordonnées depuis le formulaire (sélecteur de carte ou nettoyage)
        existing.setLatitude(updated.getLatitude());
        existing.setLongitude(updated.getLongitude());
        // Géocoder l'adresse si elle est renseignée mais sans coordonnées
        geocodeIfNeeded(existing);
        existing.setComment(normalizeComment(updated.getComment()));
        return plannerEventRepository.save(existing);
    }

    @Transactional
    public void delete(Long eventId) {
        plannerEventRepository.deleteById(eventId);
    }

    /**
     * Tente de géocoder l'adresse texte de l'événement si :
     * - un lieu / adresse est renseigné
     * - les coordonnées ne sont pas déjà définies (saisie manuelle via la carte)
     * Si le géocodage est désactivé ou échoue, les coordonnées restent nulles.
     */
    private void geocodeIfNeeded(PlannerEvent event) {
        if (event.getLatitude() != null && event.getLongitude() != null) {
            return; // Coordonnées déjà présentes (sélecteur de carte)
        }
        String location = event.getLocation();
        if (location == null || location.isBlank()) {
            return; // Pas d'adresse → rien à géocoder
        }
        ForwardGeocodingService.GeocodingResult result = forwardGeocodingService.geocode(location);
        if (result != null) {
            event.setLatitude(result.latitude());
            event.setLongitude(result.longitude());
        }
    }

    static String normalizeComment(String comment) {
        if (comment == null || "undefined".equals(comment)) {
            return "";
        }
        return comment;
    }
}
