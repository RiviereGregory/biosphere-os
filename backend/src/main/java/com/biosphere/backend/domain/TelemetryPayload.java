package com.biosphere.backend.domain;

public record TelemetryPayload(
        float temperature,
        float humidity,
        int soil
) {}
