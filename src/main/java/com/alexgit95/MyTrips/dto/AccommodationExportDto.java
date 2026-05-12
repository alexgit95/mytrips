package com.alexgit95.MyTrips.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccommodationExportDto {
    private Long id;
    private String name;
    private String address;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private Double latitude;
    private Double longitude;
    private String comment;
    private Long tripId;
}
