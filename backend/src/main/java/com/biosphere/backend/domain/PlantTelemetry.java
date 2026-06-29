package com.biosphere.backend.domain;

import java.time.LocalDateTime;

public record PlantTelemetry(float temperature,
                             float airHumidity,
                             int soilMoisture,
                             LocalDateTime timestamp) {
}
