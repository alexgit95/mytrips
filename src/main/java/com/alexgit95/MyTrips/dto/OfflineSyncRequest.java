package com.alexgit95.MyTrips.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfflineSyncRequest {
    private List<OfflineAction> actions;
}
