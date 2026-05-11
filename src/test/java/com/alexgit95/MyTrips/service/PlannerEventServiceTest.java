package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import com.alexgit95.MyTrips.service.TripService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerEventServiceTest {

    @Mock
    private PlannerEventRepository plannerEventRepository;

    @Mock
    private TripService tripService;

    @InjectMocks
    private PlannerEventService service;

    @Test
    void update_shouldPersistEmptyCommentWhenFrontendSendsUndefined() {
        PlannerEvent existing = PlannerEvent.builder()
                .id(10L)
                .name("Visite")
                .eventDateTime(LocalDateTime.of(2026, 5, 4, 9, 0))
                .comment("Ancien commentaire")
                .build();
        PlannerEvent updated = PlannerEvent.builder()
                .name("Visite")
                .eventDateTime(LocalDateTime.of(2026, 5, 4, 10, 0))
                .comment("undefined")
                .build();

        when(plannerEventRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(plannerEventRepository.save(existing)).thenReturn(existing);

        service.update(10L, updated);

        assertEquals("", existing.getComment());
        verify(plannerEventRepository).save(existing);
    }

    @Test
    void create_shouldPersistEmptyCommentWhenCommentIsNull() {
        PlannerEvent event = PlannerEvent.builder()
                .name("Check-in")
                .eventDateTime(LocalDateTime.of(2026, 5, 4, 12, 0))
                .comment(null)
                .build();
        Trip trip = Trip.builder().id(3L).build();

        when(tripService.findById(3L)).thenReturn(trip);
        when(plannerEventRepository.save(event)).thenReturn(event);

        service.create(3L, event);

        ArgumentCaptor<PlannerEvent> captor = ArgumentCaptor.forClass(PlannerEvent.class);
        verify(plannerEventRepository).save(captor.capture());
        assertEquals("", captor.getValue().getComment());
        assertEquals(trip, captor.getValue().getTrip());
    }

    @Test
    void update_shouldClearCachedCoordinatesWhenLocationChanges() {
        PlannerEvent existing = PlannerEvent.builder()
                .id(11L)
                .name("Spot")
                .eventDateTime(LocalDateTime.of(2026, 5, 4, 9, 0))
                .location("Paris")
                .latitude(48.8566)
                .longitude(2.3522)
                .build();
        PlannerEvent updated = PlannerEvent.builder()
                .name("Spot")
                .eventDateTime(LocalDateTime.of(2026, 5, 4, 10, 0))
                .location("Lyon")
                .build();

        when(plannerEventRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(plannerEventRepository.save(existing)).thenReturn(existing);

        service.update(11L, updated);

        assertNull(existing.getLatitude());
        assertNull(existing.getLongitude());
        verify(plannerEventRepository).save(existing);
    }
}