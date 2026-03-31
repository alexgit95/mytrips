package com.alexgit95.MyTrips.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataImportExportServiceTest {

    @Mock
    private ImportExportWorker worker;

    @InjectMocks
    private DataImportExportService service;

    @Test
    void exportToJson_shouldDelegateToWorker() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        service.exportToJson(out);

        verify(worker).exportToJson(out);
    }

    @Test
    void importFromJson_shouldDelegateToWorker() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream("{}".getBytes());

        service.importFromJson(in);

        verify(worker).importFromJson(in);
    }

    @Test
    void importFromJson_shouldPropagateIOException() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream("{}".getBytes());
        doThrow(new IOException("boom")).when(worker).importFromJson(in);

        assertThrows(IOException.class, () -> service.importFromJson(in));
    }
}
