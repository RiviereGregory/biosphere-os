package com.biosphere.backend.service;

import com.biosphere.backend.domain.PlantTelemetry;
import com.biosphere.backend.domain.TelemetryEntity;
import com.biosphere.backend.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TelemetryIngestService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestService.class);
    private final TelemetryRepository repository;
    private final SseNotificationService sseService;

    // Injection de dépendance via le constructeur
    public TelemetryIngestService(TelemetryRepository repository, SseNotificationService sseService) {
        this.repository = repository;
        this.sseService = sseService;
    }


    public void ingest(PlantTelemetry telemetry, String source) {
        // Le Service transforme l'objet métier en objet d'infrastructure (Entity)
        TelemetryEntity entity = new TelemetryEntity(
                telemetry.temperature(),
                telemetry.airHumidity(),
                telemetry.soilMoisture(),
                source,
                telemetry.timestamp()
        );
        repository.save(entity);
        // On pousse la nouvelle donnée vers les navigateurs !
        sseService.dispatch(entity);

        log.info("💾 [Sauvegarde BDD] -> Origine: {} | Temp: {}°C", source, telemetry.temperature());
    }

    // Parseur spécifique pour le format série de l'Arduino
    public void ingestRawSerialLine(String rawLine) {
        try {
            String[] parts = rawLine.trim().split("\\|");
            float temp = Float.parseFloat(parts[0].split(":")[1]);
            float hum  = Float.parseFloat(parts[1].split(":")[1]);
            int soil   = Integer.parseInt(parts[2].split(":")[1]);

            // On appelle la méthode universelle avec le tag "USB"
            PlantTelemetry telemetry = new PlantTelemetry(temp, hum, soil, LocalDateTime.now());
            ingest(telemetry, "USB");
        } catch (Exception e) {
            log.debug("Trame ignorée (corrompue): {}", rawLine);
        }
    }
}