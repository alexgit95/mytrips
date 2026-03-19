package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.model.CategoryEntity;
import com.alexgit95.MyTrips.service.CategoryService;
import com.alexgit95.MyTrips.service.DataImportExportService;
import com.alexgit95.MyTrips.service.GeoCountryResolver;
import com.alexgit95.MyTrips.service.ReverseGeocodingService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DataImportExportService dataService;
    private final CategoryService         categoryService;
    private final GeoCountryResolver      geoCountryResolver;
    private final ReverseGeocodingService reverseGeocodingService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("geoMode", geoCountryResolver.getMode());
        model.addAttribute("geoApiEnabled", geoCountryResolver.isApiEnabled());
        model.addAttribute("geocodingEnabled", reverseGeocodingService.isEnabled());
        return "admin/index";
    }

    @PostMapping("/geo-mode")
    public String changeGeoMode(@RequestParam("apiEnabled") boolean apiEnabled,
                                RedirectAttributes ra) {
        geoCountryResolver.setApiEnabled(apiEnabled);
        String label = apiEnabled ? "API BigDataCloud" : "Local (hors-ligne)";
        ra.addFlashAttribute("geoModeSuccess",
                "Mode de résolution géographique changé en : " + label);
        return "redirect:/admin";
    }

    @PostMapping("/reverse-geocoding")
    public String changeReverseGeocoding(@RequestParam("enabled") boolean enabled,
                                          RedirectAttributes ra) {
        reverseGeocodingService.setEnabled(enabled);
        String label = enabled ? "Nominatim (adresses)" : "Désactivé (coordonnées GPS)";
        ra.addFlashAttribute("geocodingSuccess",
                "Géocodage inverse changé en : " + label);
        return "redirect:/admin";
    }

    // ===== CATÉGORIES CRUD =====

    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @GetMapping("/categories/new")
    public String newCategoryForm(Model model) {
        model.addAttribute("category", new CategoryEntity());
        model.addAttribute("pageTitle", "Nouvelle catégorie");
        return "admin/category-form";
    }

    @PostMapping("/categories")
    public String createCategory(@Valid @ModelAttribute("category") CategoryEntity category,
                                  BindingResult result,
                                  RedirectAttributes ra,
                                  Model model) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Nouvelle catégorie");
            return "admin/category-form";
        }
        categoryService.save(category);
        ra.addFlashAttribute("success", "Catégorie créée !");
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/{id}/edit")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        CategoryEntity category = categoryService.findById(id);
        model.addAttribute("category", category);
        model.addAttribute("pageTitle", "Modifier la catégorie");
        return "admin/category-form";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Long id,
                                  @Valid @ModelAttribute("category") CategoryEntity category,
                                  BindingResult result,
                                  RedirectAttributes ra,
                                  Model model) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Modifier la catégorie");
            return "admin/category-form";
        }
        category.setId(id);
        categoryService.save(category);
        ra.addFlashAttribute("success", "Catégorie mise à jour !");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("success", "Catégorie supprimée.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ---------------------------
    // Export JSON
    // ---------------------------
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        try {
            String filename = "mytrips-export-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    + ".json";
            response.setContentType("application/json");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            
            try (var out = response.getOutputStream()) {
                dataService.exportToJson(out);
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'export JSON : " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur lors de l'export : " + e.getMessage());
        }
    }

    // ---------------------------
    // Import JSON
    // ---------------------------
    @PostMapping("/import")
    public String importData(@RequestParam("file") MultipartFile file,
                             RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Veuillez sélectionner un fichier.");
            return "redirect:/admin";
        }
        try {
            System.out.println("[CONTROLLER] Début de l'import...");
            dataService.importFromJson(file.getInputStream());
            System.out.println("[CONTROLLER] Import réussi !");
            ra.addFlashAttribute("success", "Import réussi ! Toutes les données ont été remplacées.");
        } catch (Exception e) {
            System.err.println("[CONTROLLER] Erreur lors de l'import : " + e.getMessage());
            e.printStackTrace();
            ra.addFlashAttribute("error", "Erreur lors de l'import : " + e.getMessage());
        }
        return "redirect:/admin";
    }
}
