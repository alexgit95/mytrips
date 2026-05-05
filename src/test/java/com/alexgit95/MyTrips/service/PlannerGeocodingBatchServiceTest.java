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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        when(forwardGeocodingService.geocode("Zion National Park")).thenReturn(
                new ForwardGeocodingService.GeocodingResult(37.3, -113.0, "Zion National Park, Utah"));

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

    @Test
    void stopManualBatch_shouldReturnNotRunningWhenIdle() {
        PlannerGeocodingBatchService.StopResult result = service.stopManualBatch();
        assertEquals(PlannerGeocodingBatchService.StopResult.NOT_RUNNING, result);
    }

    @Test
    void stopManualBatch_shouldRequestStopWhenRunning() throws Exception {
        when(forwardGeocodingService.isEnabled()).thenReturn(true);

        PlannerEvent event = PlannerEvent.builder()
                .id(8L)
                .name("Block")
                .location("Somewhere")
                .build();

        when(plannerEventRepository.findAllNeedingGeocoding()).thenReturn(List.of(event));

        CountDownLatch enteredGeocode = new CountDownLatch(1);
        CountDownLatch allowGeocodeToFinish = new CountDownLatch(1);
        when(forwardGeocodingService.geocode("Somewhere")).thenAnswer(invocation -> {
            enteredGeocode.countDown();
            allowGeocodeToFinish.await(1, TimeUnit.SECONDS);
            return new ForwardGeocodingService.GeocodingResult(1.0, 2.0, "Somewhere, World");
        });

        assertEquals(PlannerGeocodingBatchService.StartResult.STARTED, service.startManualBatch());
        assertTrue(enteredGeocode.await(1, TimeUnit.SECONDS));

        PlannerGeocodingBatchService.StopResult stop = service.stopManualBatch();
        assertEquals(PlannerGeocodingBatchService.StopResult.STOP_REQUESTED, stop);
        assertTrue(service.getProgress().isStopRequested());

        allowGeocodeToFinish.countDown();
        waitUntilDone(service);
        assertFalse(service.getProgress().isRunning());
    }

    @Test
    void waitForRateLimit_shouldSleepInChunksUntilNextRequestIsAllowed() throws Exception {
        TestablePlannerGeocodingBatchService throttledService =
                new TestablePlannerGeocodingBatchService(plannerEventRepository, forwardGeocodingService);
        throttledService.nowMillis = 1000L;

        assertTrue(throttledService.waitForRateLimit(1250L));
        assertEquals(List.of(100L, 100L, 50L), throttledService.recordedSleeps);
        assertEquals(1250L, throttledService.nowMillis);
    }

    @Test
    void waitForRateLimit_shouldStopQuicklyWhenStopIsRequestedDuringWait() throws Exception {
        TestablePlannerGeocodingBatchService throttledService =
                new TestablePlannerGeocodingBatchService(plannerEventRepository, forwardGeocodingService);
        throttledService.nowMillis = 1000L;
        throttledService.stopAfterFirstSleep = true;

        assertFalse(throttledService.waitForRateLimit(2000L));
        assertEquals(List.of(100L), throttledService.recordedSleeps);
    }

    private void waitUntilDone(PlannerGeocodingBatchService batchService) {
        long deadline = System.currentTimeMillis() + 2000;
        while (batchService.getProgress().isRunning() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
    }

    private static final class TestablePlannerGeocodingBatchService extends PlannerGeocodingBatchService {
        private long nowMillis;
        private boolean stopAfterFirstSleep;
        private final List<Long> recordedSleeps = new CopyOnWriteArrayList<>();

        private TestablePlannerGeocodingBatchService(PlannerEventRepository plannerEventRepository,
                                                     ForwardGeocodingService forwardGeocodingService) {
            super(plannerEventRepository, forwardGeocodingService);
        }

        @Override
        long currentTimeMillis() {
            return nowMillis;
        }

        @Override
        void sleepMillis(long delayMillis) {
            recordedSleeps.add(delayMillis);
            nowMillis += delayMillis;
            if (stopAfterFirstSleep) {
                requestStop();
            }
        }
    }
}
