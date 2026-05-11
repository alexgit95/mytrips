package com.alexgit95.MyTrips.dto;

import com.alexgit95.MyTrips.model.CategoryEntity;

import java.math.BigDecimal;

public record CategorySumDto(CategoryEntity category, BigDecimal amount) {
}
