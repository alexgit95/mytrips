package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.model.Expense;
import com.alexgit95.MyTrips.model.Trip;
import com.alexgit95.MyTrips.service.CategoryService;
import com.alexgit95.MyTrips.service.ExpenseService;
import com.alexgit95.MyTrips.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ExpenseController {

    private final TripService       tripService;
    private final ExpenseService    expenseService;
    private final CategoryService   categoryService;

    // ---------------------------
    // Formulaire ajout dépense
    // ---------------------------
    @GetMapping("/trips/{tripId}/expenses/new")
    public String newForm(@PathVariable Long tripId, Model model) {
        Trip trip = tripService.findById(tripId);
        Expense expense = new Expense();
        expense.setTrip(trip);
        model.addAttribute("trip", trip);
        model.addAttribute("expense", expense);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("pageTitle", "Nouvelle dépense");
        return "expenses/form";
    }

    // ---------------------------
    // Sauvegarde ajout
    // ---------------------------
    @PostMapping("/trips/{tripId}/expenses")
    public String create(@PathVariable Long tripId,
                         @Valid @ModelAttribute("expense") Expense expense,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        Trip trip = tripService.findById(tripId);
        expense.setTrip(trip);

        if (result.hasErrors()) {
            model.addAttribute("trip", trip);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("pageTitle", "Nouvelle dépense");
            return "expenses/form";
        }
        expenseService.save(expense);
        ra.addFlashAttribute("success", "Dépense ajoutée !");
        return "redirect:/trips/" + tripId;
    }

    // ---------------------------
    // Formulaire modification
    // ---------------------------
    @GetMapping("/trips/{tripId}/expenses/{id}/edit")
    public String editForm(@PathVariable Long tripId,
                           @PathVariable Long id,
                           Model model) {
        Trip    trip    = tripService.findById(tripId);
        Expense expense = expenseService.findById(id);
        model.addAttribute("trip", trip);
        model.addAttribute("expense", expense);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("pageTitle", "Modifier la dépense");
        return "expenses/form";
    }

    // ---------------------------
    // Sauvegarde modification
    // ---------------------------
    @PostMapping("/trips/{tripId}/expenses/{id}")
    public String update(@PathVariable Long tripId,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("expense") Expense expense,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        Trip trip = tripService.findById(tripId);
        expense.setId(id);
        expense.setTrip(trip);

        if (result.hasErrors()) {
            model.addAttribute("trip", trip);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("pageTitle", "Modifier la dépense");
            return "expenses/form";
        }
        expenseService.save(expense);
        ra.addFlashAttribute("success", "Dépense mise à jour !");
        return "redirect:/trips/" + tripId;
    }

    // ---------------------------
    // Suppression
    // ---------------------------
    @PostMapping("/trips/{tripId}/expenses/{id}/delete")
    public String delete(@PathVariable Long tripId,
                         @PathVariable Long id,
                         RedirectAttributes ra) {
        expenseService.delete(id);
        ra.addFlashAttribute("success", "Dépense supprimée.");
        return "redirect:/trips/" + tripId;
    }
}
