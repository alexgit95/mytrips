package com.alexgit95.MyTrips.repository;

import com.alexgit95.MyTrips.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findAllByOrderByStartDateDesc();
    List<Trip> findAllByOrderByStartDateAsc();
    Optional<Trip> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(LocalDate today1, LocalDate today2);
}
