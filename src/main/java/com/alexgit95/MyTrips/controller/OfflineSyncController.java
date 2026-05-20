package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.dto.OfflineAction;
import com.alexgit95.MyTrips.dto.OfflineSyncRequest;
import com.alexgit95.MyTrips.dto.OfflineSyncResponse;
import com.alexgit95.MyTrips.service.OfflineSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/offline")
@RequiredArgsConstructor
@Slf4j
public class OfflineSyncController {

    private final OfflineSyncService offlineSyncService;

    @PreAuthorize("hasAnyRole('ADMIN', 'REPORTER')")
    @PostMapping("/sync")
    public ResponseEntity<OfflineSyncResponse> sync(@RequestBody OfflineSyncRequest request) {
        if (request.getActions() == null || request.getActions().isEmpty()) {
            return ResponseEntity.ok(OfflineSyncResponse.builder()
                    .synced(0)
                    .failed(0)
                    .message("Aucune action à synchroniser")
                    .build());
        }

        log.info("[OfflineSync] Received {} actions to sync", request.getActions().size());

        int synced = 0;
        int failed = 0;

        for (OfflineAction action : request.getActions()) {
            try {
                offlineSyncService.processAction(action);
                synced++;
            } catch (Exception e) {
                log.error("[OfflineSync] Failed to process action: type={}, tripId={}, error={}",
                        action.getType(), action.getTripId(), e.getMessage());
                failed++;
            }
        }

        log.info("[OfflineSync] Sync complete: synced={}, failed={}", synced, failed);

        return ResponseEntity.ok(OfflineSyncResponse.builder()
                .synced(synced)
                .failed(failed)
                .message(failed == 0 ? "Synchronisation réussie" : "Synchronisation partielle")
                .build());
    }
}
