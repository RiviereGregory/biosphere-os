package com.biosphere.backend.controller;

import com.biosphere.backend.service.ArduinoListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/actuators")
public class ActuatorController {

    private static final Logger log = LoggerFactory.getLogger(ActuatorController.class);
    private final ArduinoListenerService hardwareService;

    public ActuatorController(ArduinoListenerService hardwareService) {
        this.hardwareService = hardwareService;
    }

    // Endpoint POST : /api/actuators/led?state=true
    @PostMapping("/led")
    public ResponseEntity<String> toggleLed(@RequestParam boolean state) {
        log.info("🕹️ [API REST] Demande de modification d'état reçue : LED -> {}", state ? "ON" : "OFF");

        // Routage de la commande vers le protocole série
        if (state) {
            hardwareService.sendCommand("CMD:LED:ON");
        } else {
            hardwareService.sendCommand("CMD:LED:OFF");
        }

        return ResponseEntity.ok("Commande d'actionneur transmise avec succès.");
    }
}