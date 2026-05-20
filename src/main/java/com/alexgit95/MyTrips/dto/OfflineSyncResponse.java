package com.alexgit95.MyTrips.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSyncResponse {
    private int synced;
    private int failed;
    private String message;
}
