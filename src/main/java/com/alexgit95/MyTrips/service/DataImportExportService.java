package com.alexgit95.MyTrips.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Service
@RequiredArgsConstructor
public class DataImportExportService {

    private final ImportExportWorker importExportWorker;

    // ----------------------------------------------------------------
    // Export: serialise toutes les données en JSON vers l'OutputStream
    // ----------------------------------------------------------------
    public void exportToJson(OutputStream out) throws IOException {
        importExportWorker.exportToJson(out);
    }

    // ----------------------------------------------------------------
    // Import: remplace toutes les données par celles du JSON fourni
    // ----------------------------------------------------------------
    public void importFromJson(InputStream in) throws IOException {
        importExportWorker.importFromJson(in);
    }
}
