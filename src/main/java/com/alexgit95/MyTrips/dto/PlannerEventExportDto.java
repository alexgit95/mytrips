package com.alexgit95.MyTrips.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannerEventExportDto {
    private Long id;
    private String name;
    private LocalDateTime eventDateTime;
    private String location;
    private String comment;
    private Long tripId;
}
