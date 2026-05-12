package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.Accommodation;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.AccommodationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final TripService tripService;

    @Transactional(readOnly = true)
    public List<Accommodation> findByTrip(Long tripId) {
        return accommodationRepository.findByTripIdOrderByArrivalDateAsc(tripId);
    }

    @Transactional(readOnly = true)
    public Accommodation findById(Long id) {
        return accommodationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Logement introuvable : " + id));
    }

    /**
     * Retourne le logement actif pour une date donnée (arrivalDate <= date < departureDate).
     */
    @Transactional(readOnly = true)
    public Optional<Accommodation> findActiveOnDate(Long tripId, LocalDate date) {
        return findByTrip(tripId).stream()
                .filter(a -> !a.getArrivalDate().isAfter(date) && a.getDepartureDate().isAfter(date))
                .findFirst();
    }

    @Transactional
    public Accommodation create(Long tripId, Accommodation accommodation) {
        Trip trip = tripService.findById(tripId);
        accommodation.setTrip(trip);
        return accommodationRepository.save(accommodation);
    }

    @Transactional
    public Accommodation update(Long accommodationId, Accommodation updated) {
        Accommodation existing = findById(accommodationId);
        existing.setName(updated.getName());
        existing.setAddress(updated.getAddress());
        existing.setArrivalDate(updated.getArrivalDate());
        existing.setDepartureDate(updated.getDepartureDate());
        existing.setLatitude(updated.getLatitude());
        existing.setLongitude(updated.getLongitude());
        existing.setComment(updated.getComment());
        return accommodationRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        accommodationRepository.deleteById(id);
    }
}
