package com.alexgit95.MyTrips.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TripTest {

    @Test
    void isOngoing_shouldReturnTrueWhenTodayIsWithinRange() {
        Trip trip = Trip.builder()
                .name("Current Trip")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build();

        assertTrue(trip.isOngoing());
    }

    @Test
    void isOngoing_shouldReturnFalseWhenTripHasNotStartedYet() {
        Trip trip = Trip.builder()
                .name("Future Trip")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        assertFalse(trip.isOngoing());
    }

    @Test
    void isOngoing_shouldReturnFalseWhenTripIsFinished() {
        Trip trip = Trip.builder()
                .name("Past Trip")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(1))
                .build();

        assertFalse(trip.isOngoing());
    }
}
