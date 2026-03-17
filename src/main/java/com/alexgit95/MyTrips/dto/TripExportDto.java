package com.alexgit95.MyTrips.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripExportDto {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private String imageUrl;
    private BigDecimal dailyBudgetThreshold;
    private BigDecimal dailyExpenseBudget;
    private Double latitude;
    private Double longitude;
}
