package com.biosphere.backend.service;

import com.biosphere.backend.domain.PlantTelemetry;
import com.fazecast.jSerialComm.SerialPort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

@Service
public class ArduinoListenerService {

    private static final Logger log = LoggerFactory.getLogger(ArduinoListenerService.class);
    private SerialPort activePort;

    @PostConstruct
    public void startListening() {
        final SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            log.error("❌ Aucun port série détecté par le noyau Linux !");
            return;
        }

        // 1. Stratégie anti-fantôme Linux : on cherche un port USB/ACM
        // (Linux liste parfois des ports ttyS0 à ttyS31 vides sur la carte mère)
        for (final SerialPort port : ports) {
            final String name = port.getSystemPortName();
            if (name.contains("ACM") || name.contains("USB")) {
                activePort = port;
                break;
            }
        }

        // Fallback si la détection stricte a échoué
        if (activePort == null){
            activePort = ports[0];
        }

        log.info("🔌 Port cible identifié : /dev/{}", activePort.getSystemPortName());

        activePort.setBaudRate(9600);
        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        if (!activePort.openPort()) {
            log.error("❌ Échec d'ouverture du port. Vérifie l'appartenance au groupe Linux 'dialout' !");
            return;
        }

        log.info("✅ Liaison série établie ! Lancement du Virtual Thread Loom...");

        // 2. Lancement du Thread Virtuel léger managé par la JVM
        Thread.ofVirtual().name("arduino-vthread").start(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(activePort.getInputStream()))) {
                String rawLine;
                while ((rawLine = reader.readLine()) != null) {
                    processIncomingLine(rawLine);
                }
            } catch (Exception e) {
                log.error("💥 Déconnexion critique du flux USB", e);
            }
        });
    }

    private void processIncomingLine(final String rawLine) {
        // Trame attendue : "T:22.5|H:55.0|S:66"
        try {
            String[] parts = rawLine.trim().split("\\|");
            float temp = Float.parseFloat(parts[0].split(":")[1]);
            float hum  = Float.parseFloat(parts[1].split(":")[1]);
            int soil   = Integer.parseInt(parts[2].split(":")[1]);

            PlantTelemetry telemetry = new PlantTelemetry(temp, hum, soil, LocalDateTime.now());

            // C'est ici qu'à la Semaine 3 nous appellerons le Repository PostgreSQL !
            log.info("🌿 [Télémétrie Reçue] -> {}", telemetry);

        } catch (Exception e) {
            // Rejet silencieux des trames incomplètes lors du branchement à chaud
            log.debug("Trame ignorée (corrompue) : {}", rawLine);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (activePort != null && activePort.isOpen()) {
            activePort.closePort();
            log.info("🔌 Fermeture propre du port série.");
        }
    }
}