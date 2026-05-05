package com.alexgit95.MyTrips.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for trip markers on the world map.
 * Represents either a trip main location or a planner event location.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripMarkerDto {
    private Long tripId;
    private String tripName;
    private String tripImageUrl;
    private String color;  // Hexadecimal color for this trip (to distinguish markers)
    private Double latitude;
    private Double longitude;
    private String title;  // Location name or stage name
    private String location;  // Free text location
    private String markerType;  // "trip" or "event" (stage)
    private String eventName;  // Name of the planner event (if event marker)
}
