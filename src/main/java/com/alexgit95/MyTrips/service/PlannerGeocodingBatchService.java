package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.repository.PlannerEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PlannerGeocodingBatchService {

    private static final Logger log = LoggerFactory.getLogger(PlannerGeocodingBatchService.class);
    private static final long NOMINATIM_MIN_DELAY_MS = 1000L;
    private static final long STOP_CHECK_INTERVAL_MS = 100L;

    private final PlannerEventRepository plannerEventRepository;
    private final ForwardGeocodingService forwardGeocodingService;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicLong runTargetEvents = new AtomicLong(0);
    private final AtomicLong processedInCurrentRun = new AtomicLong(0);
    private final AtomicLong geocodedInCurrentRun = new AtomicLong(0);
    private final AtomicLong failedInCurrentRun = new AtomicLong(0);

    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime finishedAt;
    private volatile String lastMessage = "Aucun lancement";

    public PlannerGeocodingBatchService(PlannerEventRepository plannerEventRepository,
                                        ForwardGeocodingService forwardGeocodingService) {
        this.plannerEventRepository = plannerEventRepository;
        this.forwardGeocodingService = forwardGeocodingService;
    }

    public StartResult startManualBatch() {
        if (!forwardGeocodingService.isEnabled()) {
            return StartResult.GEOCODING_DISABLED;
        }
        if (!running.compareAndSet(false, true)) {
            return StartResult.ALREADY_RUNNING;
        }

        List<PlannerEvent> candidates = plannerEventRepository.findAllNeedingGeocoding();
        runTargetEvents.set(candidates.size());
        stopRequested.set(false);
        processedInCurrentRun.set(0);
        geocodedInCurrentRun.set(0);
        failedInCurrentRun.set(0);
        startedAt = LocalDateTime.now();
        finishedAt = null;
        lastMessage = "Traitement en cours";

        CompletableFuture.runAsync(() -> runBatch(candidates));
        return StartResult.STARTED;
    }

    public StopResult stopManualBatch() {
        if (!running.get()) {
            return StopResult.NOT_RUNNING;
        }
        requestStop();
        lastMessage = "Arret demande, finalisation en cours";
        return StopResult.STOP_REQUESTED;
    }

    public Progress getProgress() {
        long totalEvents = plannerEventRepository.countEventsWithLocation();
        long geocodedEvents = plannerEventRepository.countEventsWithCoordinates();
        long pendingEvents = Math.max(0, totalEvents - geocodedEvents);

        return new Progress(
                running.get(),
                stopRequested.get(),
                totalEvents,
                geocodedEvents,
                pendingEvents,
                runTargetEvents.get(),
                processedInCurrentRun.get(),
                geocodedInCurrentRun.get(),
                failedInCurrentRun.get(),
                startedAt,
                finishedAt,
                lastMessage
        );
    }

    private void runBatch(List<PlannerEvent> candidates) {
        long nextAllowedRequestAtMillis = 0L;

        try {
            for (PlannerEvent event : candidates) {
                if (stopRequested.get()) {
                    lastMessage = "Traitement interrompu par l'utilisateur";
                    break;
                }
                if (!forwardGeocodingService.isEnabled()) {
                    lastMessage = "Traitement interrompu: geocodage desactive";
                    break;
                }

                String location = event.getLocation();
                if (location == null || location.isBlank()) {
                    processedInCurrentRun.incrementAndGet();
                    continue;
                }

                if (!waitForRateLimit(nextAllowedRequestAtMillis)) {
                    lastMessage = "Traitement interrompu par l'utilisateur";
                    break;
                }

                double[] coords = forwardGeocodingService.geocode(location);
                nextAllowedRequestAtMillis = currentTimeMillis() + NOMINATIM_MIN_DELAY_MS;
                if (coords != null && coords.length >= 2) {
                    event.setLatitude(coords[0]);
                    event.setLongitude(coords[1]);
                    plannerEventRepository.save(event);
                    geocodedInCurrentRun.incrementAndGet();
                } else {
                    failedInCurrentRun.incrementAndGet();
                }
                processedInCurrentRun.incrementAndGet();
            }

            if (!stopRequested.get() && forwardGeocodingService.isEnabled()) {
                lastMessage = "Traitement termine";
            }
        } catch (Exception e) {
            lastMessage = "Erreur pendant le traitement";
            log.error("Erreur pendant le batch manuel de geocodage planner", e);
        } finally {
            finishedAt = LocalDateTime.now();
            stopRequested.set(false);
            running.set(false);
        }
    }

    boolean waitForRateLimit(long nextAllowedRequestAtMillis) {
        while (!stopRequested.get()) {
            long remainingMillis = nextAllowedRequestAtMillis - currentTimeMillis();
            if (remainingMillis <= 0) {
                return true;
            }

            long sleepMillis = Math.min(remainingMillis, STOP_CHECK_INTERVAL_MS);
            try {
                sleepMillis(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Batch manuel interrompu pendant l'attente du rate limit Nominatim");
                return false;
            }
        }
        return false;
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    void sleepMillis(long delayMillis) throws InterruptedException {
        Thread.sleep(delayMillis);
    }

    void requestStop() {
        stopRequested.set(true);
    }

    public enum StartResult {
        STARTED,
        ALREADY_RUNNING,
        GEOCODING_DISABLED
    }

    public enum StopResult {
        STOP_REQUESTED,
        NOT_RUNNING
    }

    public static final class Progress {
        private final boolean running;
        private final boolean stopRequested;
        private final long totalEvents;
        private final long geocodedEvents;
        private final long pendingEvents;
        private final long runTargetEvents;
        private final long processedInCurrentRun;
        private final long geocodedInCurrentRun;
        private final long failedInCurrentRun;
        private final LocalDateTime startedAt;
        private final LocalDateTime finishedAt;
        private final String lastMessage;

        public Progress(boolean running,
                        boolean stopRequested,
                        long totalEvents,
                        long geocodedEvents,
                        long pendingEvents,
                        long runTargetEvents,
                        long processedInCurrentRun,
                        long geocodedInCurrentRun,
                        long failedInCurrentRun,
                        LocalDateTime startedAt,
                        LocalDateTime finishedAt,
                        String lastMessage) {
            this.running = running;
            this.stopRequested = stopRequested;
            this.totalEvents = totalEvents;
            this.geocodedEvents = geocodedEvents;
            this.pendingEvents = pendingEvents;
            this.runTargetEvents = runTargetEvents;
            this.processedInCurrentRun = processedInCurrentRun;
            this.geocodedInCurrentRun = geocodedInCurrentRun;
            this.failedInCurrentRun = failedInCurrentRun;
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
            this.lastMessage = lastMessage;
        }

        public boolean isRunning() { return running; }

    public boolean isStopRequested() { return stopRequested; }

        public long getTotalEvents() { return totalEvents; }

        public long getGeocodedEvents() { return geocodedEvents; }

        public long getPendingEvents() { return pendingEvents; }

        public long getRunTargetEvents() { return runTargetEvents; }

        public long getProcessedInCurrentRun() { return processedInCurrentRun; }

        public long getGeocodedInCurrentRun() { return geocodedInCurrentRun; }

        public long getFailedInCurrentRun() { return failedInCurrentRun; }

        public LocalDateTime getStartedAt() { return startedAt; }

        public LocalDateTime getFinishedAt() { return finishedAt; }

        public String getLastMessage() { return lastMessage; }
    }
}
