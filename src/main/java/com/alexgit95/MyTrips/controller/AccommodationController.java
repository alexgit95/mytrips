package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.model.Accommodation;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.service.AccommodationService;
import com.alexgit95.MyTrips.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/trips/{tripId}/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

    private final TripService tripService;
    private final AccommodationService accommodationService;

    // ---------------------------
    // Liste des logements
    // ---------------------------
    @GetMapping
    public String list(@PathVariable Long tripId, Model model) {
        Trip trip = tripService.findById(tripId);
        List<Accommodation> accommodations = accommodationService.findByTrip(tripId);
        model.addAttribute("trip", trip);
        model.addAttribute("accommodations", accommodations);
        model.addAttribute("newAccommodation", new Accommodation());
        return "trips/accommodations";
    }

    // ---------------------------
    // Création d'un logement
    // ---------------------------
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public String create(@PathVariable Long tripId,
                         @Valid @ModelAttribute("newAccommodation") Accommodation accommodation,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            Trip trip = tripService.findById(tripId);
            model.addAttribute("trip", trip);
            model.addAttribute("accommodations", accommodationService.findByTrip(tripId));
            return "trips/accommodations";
        }
        accommodationService.create(tripId, accommodation);
        ra.addFlashAttribute("success", "Logement ajouté !");
        return "redirect:/trips/" + tripId + "/accommodations";
    }

    // ---------------------------
    // Formulaire d'édition (GET)
    // ---------------------------
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long tripId,
                           @PathVariable Long id,
                           Model model) {
        Trip trip = tripService.findById(tripId);
        List<Accommodation> accommodations = accommodationService.findByTrip(tripId);
        Accommodation accommodation = accommodationService.findById(id);
        model.addAttribute("trip", trip);
        model.addAttribute("accommodations", accommodations);
        model.addAttribute("newAccommodation", new Accommodation());
        model.addAttribute("editAccommodation", accommodation);
        return "trips/accommodations";
    }

    // ---------------------------
    // Sauvegarde modification
    // ---------------------------
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}")
    public String update(@PathVariable Long tripId,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("editAccommodation") Accommodation updated,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            Trip trip = tripService.findById(tripId);
            model.addAttribute("trip", trip);
            model.addAttribute("accommodations", accommodationService.findByTrip(tripId));
            model.addAttribute("newAccommodation", new Accommodation());
            return "trips/accommodations";
        }
        accommodationService.update(id, updated);
        ra.addFlashAttribute("success", "Logement mis à jour !");
        return "redirect:/trips/" + tripId + "/accommodations";
    }

    // ---------------------------
    // Suppression
    // ---------------------------
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long tripId,
                         @PathVariable Long id,
                         RedirectAttributes ra) {
        accommodationService.delete(id);
        ra.addFlashAttribute("success", "Logement supprimé !");
        return "redirect:/trips/" + tripId + "/accommodations";
    }
}
