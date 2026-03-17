package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.TripRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TripService {

    private final TripRepository tripRepository;

    @Transactional(readOnly = true)
    public List<Trip> findAll() {
        return tripRepository.findAllByOrderByStartDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Trip> findAllChronological() {
        return tripRepository.findAllByOrderByStartDateAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Trip> findCurrentTrip() {
        LocalDate today = LocalDate.now();
        return tripRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today);
    }

    @Transactional(readOnly = true)
    public Trip findById(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voyage introuvable : " + id));
    }

    public Trip save(Trip trip) {
        return tripRepository.save(trip);
    }

    public void delete(Long id) {
        tripRepository.deleteById(id);
    }
}
