package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.model.PlannerEvent;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.service.PlannerEventService;
import com.alexgit95.MyTrips.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/trips/{tripId}/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final TripService tripService;
    private final PlannerEventService plannerEventService;

    // ---------------------------
    // Affichage du planner
    // ---------------------------
    @GetMapping
    public String show(@PathVariable Long tripId, Model model) {
        Trip trip = tripService.findById(tripId);
        Map<LocalDate, java.util.List<PlannerEvent>> eventsByDay =
                plannerEventService.groupByDay(tripId);

        model.addAttribute("trip", trip);
        model.addAttribute("eventsByDay", eventsByDay);
        model.addAttribute("newEvent", new PlannerEvent());
        return "trips/planner";
    }

    // ---------------------------
    // Création d'un événement
    // ---------------------------
    @PostMapping("/events")
    public String create(@PathVariable Long tripId,
                         @Valid @ModelAttribute("newEvent") PlannerEvent event,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            Trip trip = tripService.findById(tripId);
            model.addAttribute("trip", trip);
            model.addAttribute("eventsByDay", plannerEventService.groupByDay(tripId));
            return "trips/planner";
        }
        plannerEventService.create(tripId, event);
        ra.addFlashAttribute("success", "Événement ajouté !");
        return "redirect:/trips/" + tripId + "/planner";
    }

    // ---------------------------
    // Formulaire d'édition (GET)
    // ---------------------------
    @GetMapping("/events/{eventId}/edit")
    public String editForm(@PathVariable Long tripId,
                           @PathVariable Long eventId,
                           Model model) {
        Trip trip = tripService.findById(tripId);
        PlannerEvent event = plannerEventService.findById(eventId);
        Map<LocalDate, java.util.List<PlannerEvent>> eventsByDay =
                plannerEventService.groupByDay(tripId);

        model.addAttribute("trip", trip);
        model.addAttribute("eventsByDay", eventsByDay);
        model.addAttribute("newEvent", new PlannerEvent());
        model.addAttribute("editEvent", event);
        return "trips/planner";
    }

    // ---------------------------
    // Sauvegarde modification
    // ---------------------------
    @PostMapping("/events/{eventId}")
    public String update(@PathVariable Long tripId,
                         @PathVariable Long eventId,
                         @Valid @ModelAttribute("editEvent") PlannerEvent updated,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (result.hasErrors()) {
            Trip trip = tripService.findById(tripId);
            model.addAttribute("trip", trip);
            model.addAttribute("eventsByDay", plannerEventService.groupByDay(tripId));
            model.addAttribute("newEvent", new PlannerEvent());
            return "trips/planner";
        }
        plannerEventService.update(eventId, updated);
        ra.addFlashAttribute("success", "Événement mis à jour !");
        return "redirect:/trips/" + tripId + "/planner";
    }

    // ---------------------------
    // Suppression
    // ---------------------------
    @PostMapping("/events/{eventId}/delete")
    public String delete(@PathVariable Long tripId,
                         @PathVariable Long eventId,
                         RedirectAttributes ra) {
        plannerEventService.delete(eventId);
        ra.addFlashAttribute("success", "Événement supprimé.");
        return "redirect:/trips/" + tripId + "/planner";
    }
}
