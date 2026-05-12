package com.alexgit95.MyTrips.repository;

import com.alexgit95.MyTrips.model.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    List<Accommodation> findByTripIdOrderByArrivalDateAsc(Long tripId);

    List<Accommodation> findByTripId(Long tripId);
}
