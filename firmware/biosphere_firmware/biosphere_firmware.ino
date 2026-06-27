/**
 * Projet : Bio-Sphere OS (Firmware V1 - Mock Telemetry)
 * Cible  : Arduino Uno / Nano (ATmega328P)
 * Rôle   : Émet un flux série au format "T:xx.x|H:xx.x|S:xx\n" toutes les 2000ms.
 */

unsigned long previousMillis = 0;
const long interval = 2000;

// Variables d'état simulées
float mockTemp = 22.0;
float mockHumidity = 55.0;
int mockSoil = 65; // % d'humidité du sol

void setup() {
    Serial.begin(9600); // Vitesse standard et fiable
}

void loop() {
    unsigned long currentMillis = millis();

    if (currentMillis - previousMillis >= interval) {
        previousMillis = currentMillis;

        // 1. Simulation d'une petite variation naturelle
        mockTemp += random(-5, 6) / 10.0;     // +/- 0.5 °C
        mockSoil = constrain(mockSoil + random(-1, 2), 10, 95); 

        // 2. Formatage dans un buffer fixe sur la pile (Stack)
        char telemetryBuffer[40];
        
        char tempStr[8];
        dtostrf(mockTemp, 4, 1, tempStr); // 4 caractères au total, 1 décimale

        char humStr[8];
        dtostrf(mockHumidity, 4, 1, humStr);

        snprintf(telemetryBuffer, sizeof(telemetryBuffer), "T:%s|H:%s|S:%d", tempStr, humStr, mockSoil);

        // 3. Envoi physique sur le port série TX
        Serial.println(telemetryBuffer);
    }
}
