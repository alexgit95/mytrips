package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.dto.CountryStatsDto;
import com.alexgit95.MyTrips.service.WorldStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/trips/world")
@RequiredArgsConstructor
public class WorldStatsController {

    private final WorldStatsService worldStatsService;

    @GetMapping
    public String worldStats(Model model) {
        Map<String, List<CountryStatsDto>> statsByContinent = worldStatsService.computeStats();
        int totalCountries = statsByContinent.values().stream().mapToInt(List::size).sum();
        int totalContinents = statsByContinent.size();

        model.addAttribute("statsByContinent", statsByContinent);
        model.addAttribute("totalCountries", totalCountries);
        model.addAttribute("totalContinents", totalContinents);
        return "trips/worldstats";
    }
}
