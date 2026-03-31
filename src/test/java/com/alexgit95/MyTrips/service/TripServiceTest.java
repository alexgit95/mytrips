package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.TripRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TripService service;

    @Test
    void findById_shouldReturnTripWhenFound() {
        Trip trip = buildTrip(1L, "Paris");
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        Trip found = service.findById(1L);

        assertEquals("Paris", found.getName());
    }

    @Test
    void findById_shouldThrowWhenMissing() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.findById(99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void findCurrentTrip_shouldQueryRepositoryWithTodayTwice() {
        Trip trip = buildTrip(2L, "Current");
        when(tripRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(any(), any()))
                .thenReturn(Optional.of(trip));

        Optional<Trip> result = service.findCurrentTrip();

        assertTrue(result.isPresent());
        ArgumentCaptor<LocalDate> startArg = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endArg = ArgumentCaptor.forClass(LocalDate.class);
        verify(tripRepository).findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(startArg.capture(), endArg.capture());
        assertEquals(startArg.getValue(), endArg.getValue());
    }

    @Test
    void findAll_shouldDelegateToRepository() {
        when(tripRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of());

        service.findAll();

        verify(tripRepository).findAllByOrderByStartDateDesc();
    }

    @Test
    void saveAndDelete_shouldDelegateToRepository() {
        Trip trip = buildTrip(null, "Save me");
        when(tripRepository.save(trip)).thenReturn(buildTrip(10L, "Save me"));

        Trip saved = service.save(trip);
        service.delete(10L);

        assertEquals(10L, saved.getId());
        verify(tripRepository).deleteById(10L);
    }

    private Trip buildTrip(Long id, String name) {
        return Trip.builder()
                .id(id)
                .name(name)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .budget(BigDecimal.valueOf(100))
                .build();
    }
}
