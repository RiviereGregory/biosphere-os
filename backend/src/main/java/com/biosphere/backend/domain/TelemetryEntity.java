package com.biosphere.backend.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "telemetry_history")
public class TelemetryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private float temperature;
    private float humidity;
    private int soil;

    @Column(name = "data_source")
    private String source; // "USB" (Arduino) ou "WIFI" (ESP32)

    private LocalDateTime timestamp;

    // 1. Constructeur vide exigé par JPA
    public TelemetryEntity() {}

    // 2. Constructeur utile pour notre code
    public TelemetryEntity(float temperature, float humidity, int soil, String source, LocalDateTime timestamp) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.soil = soil;
        this.source = source;
        this.timestamp = timestamp;
    }

    // Getters
    public Long getId() { return id; }
    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    public int getSoil() { return soil; }
    public String getSource() { return source; }
    public LocalDateTime getTimestamp() { return timestamp; }
}