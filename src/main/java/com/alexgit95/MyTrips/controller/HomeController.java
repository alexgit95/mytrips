package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final TripService tripService;

    @GetMapping("/")
    public String home() {
        Optional<Trip> current = tripService.findCurrentTrip();
        if (current.isPresent()) {
            return "redirect:/trips/" + current.get().getId();
        }
        return "redirect:/trips";
    }
}
