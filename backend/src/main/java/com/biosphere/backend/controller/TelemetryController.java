package com.biosphere.backend.controller;

import com.biosphere.backend.domain.PlantTelemetry;
import com.biosphere.backend.domain.TelemetryPayload;
import com.biosphere.backend.service.TelemetryIngestService;
import com.biosphere.backend.domain.TelemetryEntity;
import com.biosphere.backend.repository.TelemetryRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class TelemetryController {

    private static final Logger log = LoggerFactory.getLogger(TelemetryController.class);
    private final TelemetryIngestService ingestService;
    private final TelemetryRepository repository;

    // Injection de notre service centralisé
    public TelemetryController(TelemetryIngestService ingestService, TelemetryRepository repository) {
        this.ingestService = ingestService;
        this.repository = repository;
    }

    @PostMapping("/telemetry")
    public ResponseEntity<String> receiveWifiTelemetry(@RequestBody TelemetryPayload payload) {
        // Le Controller transforme le DTO réseau en objet métier (Domain)
        PlantTelemetry telemetry = new PlantTelemetry(
                payload.temperature(),
                payload.humidity(),
                payload.soil(),
                LocalDateTime.now()
        );

        ingestService.ingest(telemetry, "WIFI");
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/telemetry")
    public ResponseEntity<List<TelemetryEntity>> getTelemetryHistory() {
        return ResponseEntity.ok(repository.findTop15ByOrderByTimestampDesc());
    }
}