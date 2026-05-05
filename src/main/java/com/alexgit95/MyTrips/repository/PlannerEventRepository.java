package com.alexgit95.MyTrips.repository;

import com.alexgit95.MyTrips.model.PlannerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlannerEventRepository extends JpaRepository<PlannerEvent, Long> {

    List<PlannerEvent> findByTripIdOrderByEventDateTimeAsc(Long tripId);

        @Query("""
                        select e from PlannerEvent e
                        where e.location is not null
                            and trim(e.location) <> ''
                            and (e.latitude is null or e.longitude is null)
                        """)
        List<PlannerEvent> findAllNeedingGeocoding();

        @Query("""
                        select count(e) from PlannerEvent e
                        where e.location is not null
                            and trim(e.location) <> ''
                        """)
        long countEventsWithLocation();

        @Query("""
                        select count(e) from PlannerEvent e
                        where e.location is not null
                            and trim(e.location) <> ''
                            and e.latitude is not null
                            and e.longitude is not null
                        """)
        long countEventsWithCoordinates();
}
