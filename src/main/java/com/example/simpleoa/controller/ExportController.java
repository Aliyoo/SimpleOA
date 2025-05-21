package com.example.simpleoa.controller;

import com.example.simpleoa.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    private final ExportService exportService;

    @Autowired
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @SuppressWarnings("unchecked") // For casting payload.get("projectsData")
    @PostMapping("/batch-fill")
    public ResponseEntity<byte[]> exportBatchFill(@RequestBody Map<String, Object> payload) {
        String startDate = (String) payload.get("startDate");
        String endDate = (String) payload.get("endDate");
        List<Map<String, Object>> projectsData = null;
        if (payload.get("projectsData") instanceof List) {
            projectsData = (List<Map<String, Object>>) payload.get("projectsData");
        }

        logger.info("Received batch fill export request for dates: {} to {}. Projects data provided: {}", 
                    startDate, endDate, (projectsData != null && !projectsData.isEmpty()));

        if (startDate == null || endDate == null || projectsData == null || projectsData.isEmpty()) {
            logger.warn("Missing required data for batch fill export: startDate={}, endDate={}, projectsDataEmpty={}",
                        startDate, endDate, projectsData == null || projectsData.isEmpty());
            return ResponseEntity.badRequest().body("Missing required data: startDate, endDate, and projectsData must be provided.".getBytes());
        }

        try {
            ByteArrayInputStream bis = exportService.generateBatchFillExcel(startDate, endDate, projectsData);
            byte[] bytes = bis.readAllBytes(); // Requires Java 9+
            // For Java 8 or older, you might use:
            // byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(bis);
            // Ensure Apache Commons IO is a dependency if you use it.

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"batch_fill_export.xlsx\"");
            headers.setContentType(MediaType.APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_SHEET);

            logger.info("Successfully generated batch fill Excel for dates: {} to {}. File size: {} bytes", startDate, endDate, bytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);
        } catch (IOException e) { // Catching IOException specifically for bis.readAllBytes()
            logger.error("IO Error during batch fill export for dates: {} to {}", startDate, endDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IO error during file generation.".getBytes());
        } catch (Exception e) {
            logger.error("Unexpected error during batch fill export for dates: {} to {}", startDate, endDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during file generation.".getBytes());
        }
    }

    @GetMapping("/statistical-report")
    public ResponseEntity<byte[]> exportStatisticalReport(@RequestParam String startDate, @RequestParam String endDate) {
        logger.info("Received statistical report export request for dates: {} to {}", startDate, endDate);

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; // Standard YYYY-MM-DD format
            LocalDate parsedStartDate = LocalDate.parse(startDate, formatter);
            LocalDate parsedEndDate = LocalDate.parse(endDate, formatter);

            if (parsedStartDate.isAfter(parsedEndDate)) {
                logger.warn("Start date {} is after end date {} for statistical report export.", parsedStartDate, parsedEndDate);
                return ResponseEntity.badRequest().body("Start date cannot be after end date.".getBytes());
            }

            ByteArrayInputStream bis = exportService.generateStatisticalReportExcel(parsedStartDate, parsedEndDate);
            byte[] bytes = bis.readAllBytes(); // Requires Java 9+
            // For Java 8 or older, you might use:
            // byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(bis);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statistical_report_export.xlsx\"");
            headers.setContentType(MediaType.APPLICATION_VND_OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_SHEET);

            logger.info("Successfully generated statistical report Excel for dates: {} to {}. File size: {} bytes", parsedStartDate, parsedEndDate, bytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);
        } catch (DateTimeParseException e) {
            logger.error("Error parsing dates for statistical report export: startDate={}, endDate={}", startDate, endDate, e);
            return ResponseEntity.badRequest().body(("Invalid date format. Please use YYYY-MM-DD. " + e.getMessage()).getBytes());
        } catch (IOException e) {
            logger.error("IO Error during statistical report export for dates: {} to {}", startDate, endDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IO error during file generation.".getBytes());
        } catch (Exception e) {
            logger.error("Unexpected error during statistical report export for dates: {} to {}", startDate, endDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during file generation.".getBytes());
        }
    }
}
