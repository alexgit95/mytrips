package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlannerGeocodingBatchServiceTest {

    @Mock
    private PlannerEventRepository plannerEventRepository;

    @Mock
    private ForwardGeocodingService forwardGeocodingService;

    private PlannerGeocodingBatchService service;

    @BeforeEach
    void setUp() {
        service = new PlannerGeocodingBatchService(plannerEventRepository, forwardGeocodingService);
    }

    @Test
    void startManualBatch_shouldRefuseWhenGeocodingDisabled() {
        when(forwardGeocodingService.isEnabled()).thenReturn(false);

        PlannerGeocodingBatchService.StartResult result = service.startManualBatch();

        assertEquals(PlannerGeocodingBatchService.StartResult.GEOCODING_DISABLED, result);
        verify(plannerEventRepository, never()).findAllNeedingGeocoding();
    }

    @Test
    void getProgress_shouldExposeDatabaseCounters() {
        when(plannerEventRepository.countEventsWithLocation()).thenReturn(10L);
        when(plannerEventRepository.countEventsWithCoordinates()).thenReturn(4L);

        PlannerGeocodingBatchService.Progress progress = service.getProgress();

        assertEquals(10L, progress.getTotalEvents());
        assertEquals(4L, progress.getGeocodedEvents());
        assertEquals(6L, progress.getPendingEvents());
    }

    @Test
    void startManualBatch_shouldGeocodeAndPersistInBackground() {
        when(forwardGeocodingService.isEnabled()).thenReturn(true);

        PlannerEvent event = PlannerEvent.builder()
                .id(5L)
                .name("Zion")
                .location("Zion National Park")
                .build();

        when(plannerEventRepository.findAllNeedingGeocoding()).thenReturn(List.of(event));
        when(forwardGeocodingService.geocode("Zion National Park")).thenReturn(new double[]{37.3, -113.0});

        PlannerGeocodingBatchService.StartResult result = service.startManualBatch();
        assertEquals(PlannerGeocodingBatchService.StartResult.STARTED, result);

        waitUntilDone(service);

        verify(plannerEventRepository, atLeastOnce()).save(any(PlannerEvent.class));

        ArgumentCaptor<PlannerEvent> captor = ArgumentCaptor.forClass(PlannerEvent.class);
        verify(plannerEventRepository, atLeastOnce()).save(captor.capture());
        PlannerEvent saved = captor.getValue();
        assertTrue(saved.getLatitude() != null);
        assertTrue(saved.getLongitude() != null);
    }

    private void waitUntilDone(PlannerGeocodingBatchService batchService) {
        long deadline = System.currentTimeMillis() + 2000;
        while (batchService.getProgress().isRunning() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
    }
}
