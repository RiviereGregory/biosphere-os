package com.biosphere.backend.service;

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
    private final TelemetryIngestService ingestService;
    private SerialPort activePort;

    public ArduinoListenerService(TelemetryIngestService ingestService) {
        this.ingestService = ingestService;
    }


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

        Thread.ofVirtual().name("arduino-vthread").start(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(activePort.getInputStream()))) {
                String rawLine;
                while ((rawLine = reader.readLine()) != null) {
                    ingestService.ingestRawSerialLine(rawLine);
                }
            } catch (Exception e) {
                log.error("💥 Déconnexion critique du flux USB", e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        if (activePort != null && activePort.isOpen()) {
            activePort.closePort();
            log.info("🔌 Fermeture propre du port série.");
        }
    }
}