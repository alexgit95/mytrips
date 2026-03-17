package com.alexgit95.MyTrips.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartDataDto {
    // All trip dates from start to end
    private List<String> labels;
    // Cumulative actual expenses, null for future dates (use null in JSON → null in Chart.js dataset)
    private List<Double> actualCumulative;
    // Flat budget line across all days
    private List<Double> budgetLine;
    // Trend projection: null for days before last expense, projected from last expense day to end
    private List<Double> trendLine;
    // Pie chart: category labels
    private List<String> categoryLabels;
    // Pie chart: category amounts
    private List<Double> categoryAmounts;
}
