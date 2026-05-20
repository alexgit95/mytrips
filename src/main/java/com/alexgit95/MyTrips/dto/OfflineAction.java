package com.alexgit95.MyTrips.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfflineAction {
    private String type; // "expense" or "planner_event"
    private Long tripId;
    private String timestamp;
    private Map<String, Object> data;
}
