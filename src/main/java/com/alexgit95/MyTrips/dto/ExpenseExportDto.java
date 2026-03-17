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
public class ExpenseExportDto {
    private Long id;
    private BigDecimal amount;
    private LocalDate date;
    private String categoryName;  // Store category name for export/import
    private String label;
    private Integer numberOfDays;
    private Long tripId;
}
