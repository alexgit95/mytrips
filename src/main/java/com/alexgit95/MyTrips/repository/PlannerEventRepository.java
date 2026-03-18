package com.alexgit95.MyTrips.repository;

import com.alexgit95.MyTrips.model.PlannerEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlannerEventRepository extends JpaRepository<PlannerEvent, Long> {

    List<PlannerEvent> findByTripIdOrderByEventDateTimeAsc(Long tripId);
}
