package com.biosphere.backend.service;

import com.biosphere.backend.domain.TelemetryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);

    // Une liste thread-safe pour stocker tous les navigateurs connectés au dashboard
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // Méthode appelée quand un Angular ouvre le tableau de bord
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // 0L = Connexion infinie
        emitters.add(emitter);

        // Nettoyage automatique si le navigateur ferme l'onglet
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    // Méthode appelée par l'ingestion pour diffuser aux navigateurs
    public void dispatch(TelemetryEntity telemetry) {
        for (SseEmitter emitter : emitters) {
            try {
                // On pousse l'événement nommé "telemetry-event" dans le tuyau HTTP
                emitter.send(SseEmitter.event()
                        .name("telemetry-event")
                        .data(telemetry));
            } catch (Exception e) {
                emitters.remove(emitter); // Si erreur réseau, on vire le client
            }
        }
    }
}