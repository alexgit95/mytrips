package com.alexgit95.MyTrips.controller;

import com.alexgit95.MyTrips.service.DataImportExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ApiExportController {

    private final DataImportExportService dataService;

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
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Erreur lors de l'export : " + e.getMessage());
        }
    }
}
