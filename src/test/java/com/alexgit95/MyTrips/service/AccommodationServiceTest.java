package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.Accommodation;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.AccommodationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccommodationServiceTest {

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private TripService tripService;

    @InjectMocks
    private AccommodationService service;

    // ────────────────────────────────────────────────────────────────
    //  findActiveOnDate
    // ────────────────────────────────────────────────────────────────

    @Test
    void findActiveOnDate_shouldReturnAccommodationWhenDateIsInRange() {
        Accommodation acc = Accommodation.builder()
                .id(1L)
                .name("Hôtel Bellevue")
                .arrivalDate(LocalDate.of(2026, 6, 1))
                .departureDate(LocalDate.of(2026, 6, 5))
                .build();

        when(accommodationRepository.findByTripIdOrderByArrivalDateAsc(10L))
                .thenReturn(List.of(acc));

        Optional<Accommodation> result = service.findActiveOnDate(10L, LocalDate.of(2026, 6, 3));

        assertTrue(result.isPresent());
        assertEquals("Hôtel Bellevue", result.get().getName());
    }

    @Test
    void findActiveOnDate_shouldReturnEmptyWhenDateEqualsArrival() {
        // arrivalDate is inclusive (the day you arrive you are "in" the accommodation)
        Accommodation acc = Accommodation.builder()
                .id(1L)
                .name("AirBnB Paris")
                .arrivalDate(LocalDate.of(2026, 6, 1))
                .departureDate(LocalDate.of(2026, 6, 4))
                .build();

        when(accommodationRepository.findByTripIdOrderByArrivalDateAsc(10L))
                .thenReturn(List.of(acc));

        Optional<Accommodation> result = service.findActiveOnDate(10L, LocalDate.of(2026, 6, 1));

        assertTrue(result.isPresent());
    }

    @Test
    void findActiveOnDate_shouldReturnEmptyWhenDateEqualsDeparture() {
        // departureDate is exclusive (the day you leave you are no longer in the accommodation)
        Accommodation acc = Accommodation.builder()
                .id(1L)
                .name("AirBnB Paris")
                .arrivalDate(LocalDate.of(2026, 6, 1))
                .departureDate(LocalDate.of(2026, 6, 4))
                .build();

        when(accommodationRepository.findByTripIdOrderByArrivalDateAsc(10L))
                .thenReturn(List.of(acc));

        Optional<Accommodation> result = service.findActiveOnDate(10L, LocalDate.of(2026, 6, 4));

        assertFalse(result.isPresent());
    }

    @Test
    void findActiveOnDate_shouldReturnEmptyWhenNoAccommodations() {
        when(accommodationRepository.findByTripIdOrderByArrivalDateAsc(10L))
                .thenReturn(List.of());

        Optional<Accommodation> result = service.findActiveOnDate(10L, LocalDate.of(2026, 6, 3));

        assertFalse(result.isPresent());
    }

    @Test
    void findActiveOnDate_shouldReturnFirstMatchWhenPeriodsOverlap() {
        Accommodation first = Accommodation.builder()
                .id(1L)
                .name("Premier logement")
                .arrivalDate(LocalDate.of(2026, 6, 1))
                .departureDate(LocalDate.of(2026, 6, 10))
                .build();
        Accommodation second = Accommodation.builder()
                .id(2L)
                .name("Deuxième logement")
                .arrivalDate(LocalDate.of(2026, 6, 5))
                .departureDate(LocalDate.of(2026, 6, 12))
                .build();

        when(accommodationRepository.findByTripIdOrderByArrivalDateAsc(10L))
                .thenReturn(List.of(first, second));

        Optional<Accommodation> result = service.findActiveOnDate(10L, LocalDate.of(2026, 6, 7));

        assertTrue(result.isPresent());
        assertEquals("Premier logement", result.get().getName());
    }

    // ────────────────────────────────────────────────────────────────
    //  create
    // ────────────────────────────────────────────────────────────────

    @Test
    void create_shouldAttachTripAndSave() {
        Trip trip = Trip.builder().id(5L).name("Voyage en Italie").build();
        Accommodation input = Accommodation.builder()
                .name("Appartement Rome")
                .arrivalDate(LocalDate.of(2026, 7, 10))
                .departureDate(LocalDate.of(2026, 7, 15))
                .build();

        when(tripService.findById(5L)).thenReturn(trip);
        when(accommodationRepository.save(input)).thenReturn(input);

        service.create(5L, input);

        ArgumentCaptor<Accommodation> captor = ArgumentCaptor.forClass(Accommodation.class);
        verify(accommodationRepository).save(captor.capture());
        assertEquals(trip, captor.getValue().getTrip());
        assertEquals("Appartement Rome", captor.getValue().getName());
    }

    // ────────────────────────────────────────────────────────────────
    //  update
    // ────────────────────────────────────────────────────────────────

    @Test
    void update_shouldModifyAllFields() {
        Accommodation existing = Accommodation.builder()
                .id(3L)
                .name("Vieux nom")
                .address("Vieille adresse")
                .arrivalDate(LocalDate.of(2026, 8, 1))
                .departureDate(LocalDate.of(2026, 8, 5))
                .latitude(43.0)
                .longitude(5.0)
                .comment("Commentaire initial")
                .build();

        Accommodation updated = Accommodation.builder()
                .name("Nouvel hôtel")
                .address("Nouvelle adresse")
                .arrivalDate(LocalDate.of(2026, 8, 2))
                .departureDate(LocalDate.of(2026, 8, 7))
                .latitude(44.0)
                .longitude(6.0)
                .comment("Nouveau commentaire")
                .build();

        when(accommodationRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(accommodationRepository.save(existing)).thenReturn(existing);

        service.update(3L, updated);

        assertEquals("Nouvel hôtel", existing.getName());
        assertEquals("Nouvelle adresse", existing.getAddress());
        assertEquals(LocalDate.of(2026, 8, 2), existing.getArrivalDate());
        assertEquals(LocalDate.of(2026, 8, 7), existing.getDepartureDate());
        assertEquals(44.0, existing.getLatitude());
        assertEquals(6.0, existing.getLongitude());
        assertEquals("Nouveau commentaire", existing.getComment());
        verify(accommodationRepository).save(existing);
    }

    // ────────────────────────────────────────────────────────────────
    //  findById — not found
    // ────────────────────────────────────────────────────────────────

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(accommodationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findById(99L));
    }

    // ────────────────────────────────────────────────────────────────
    //  delete
    // ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldCallRepositoryDeleteById() {
        service.delete(7L);
        verify(accommodationRepository).deleteById(7L);
    }
}
