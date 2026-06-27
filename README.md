# 🌿 Bio-Sphere OS — Autonomous & Resilient Greenhouse

> **Système d'exploitation local pour serre connectée autonome, résiliente et 100% souveraine (Zéro Cloud).**

---

## 🏗️ Architecture Générale


```

[ SONDRES & POMPE ]
│ (Fils électriques bruts)
▼
[ ARDUINO UNO ]  <─── (Câble USB Série : /dev/ttyACM0) ───>  [ MAC MINI LINUX ]
(C++ Embarqué)                                                (Spring Boot + BDD)

```

---

## 📅 Journal de Bord — Étape 1 : Le Contrat Matériel (Firmware V1)

L'objectif de la **Semaine 1** est de valider le pont de communication bas niveau (UART/USB) entre le microcontrôleur ATmega328P et le noyau Linux du serveur maître (Ubuntu) **sans interface graphique lourde**.

Le firmware actuel (`mock telemetry`) émet un flux de données brutes toutes les 2000ms au format :
`T:xx.x|H:xx.x|S:xx\n` *(Température en °C | Humidité Air en % | Humidité Sol en %)*.

---

## 🛠️ Procédure d'installation et de déploiement CLI (Linux / Ubuntu)

### Prérequis (Sur le Mac Mini Linux)
* Noyau Linux sain (Ubuntu Server / Desktop)
* Accès Internet pour la phase d'initialisation

### 1. Installation de l'outil officiel `arduino-cli`

```bash
# Téléchargement et placement du binaire dans le PATH système
curl -fsSL [https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh](https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh) | sh
sudo mv bin/arduino-cli /usr/local/bin/

# Initialisation de l'index et installation du core AVR (Uno/Nano)
arduino-cli core update-index
arduino-cli core install arduino:avr

```

### 2. Configuration des droits de sécurité Linux (Crucial)

Par défaut, le noyau Ubuntu bloque l'accès en lecture/écriture aux périphériques bruts `/dev/tty*` pour les utilisateurs standards.

```bash
# Ajouter l'utilisateur courant au groupe système 'dialout'
sudo usermod -a -G dialout $USER

# Appliquer les nouveaux droits immédiatement dans le shell actif
newgrp dialout

```

### 3. Compilation et Téléversement du Firmware

> **Convention stricte Arduino :** Le dossier du code source et le fichier `.ino` principal doivent porter strictement le même nom (`firmware/biosphere_firmware/biosphere_firmware.ino`).

1. **Brancher la carte Arduino en USB sur le Mac Mini.**
2. Identifier le port virtuel attribué par le noyau :
```bash
arduino-cli board list

```


*(Exemple de port identifié : `/dev/ttyACM0` ou `/dev/ttyUSB0`)*.
3. Compiler et flasher la carte en deux lignes :
```bash
# 1. Compilation
arduino-cli compile --fqbn arduino:avr:uno firmware/biosphere_firmware

# 2. Téléversement (Remplacer le port si nécessaire)
arduino-cli upload -p /dev/ttyACM0 --fqbn arduino:avr:uno firmware/biosphere_firmware

```



---

## 🧪 Test de Validation (Vérification Unix)

Sous Linux, tout périphérique étant un fichier, on écoute le flux sortant directement via les commandes systèmes standards :

```bash
# 1. Configurer la vitesse du port virtuel à 9600 bauds en mode brut (Raw)
stty -F /dev/ttyACM0 9600 raw -echo

# 2. Afficher le flux série en temps réel
cat /dev/ttyACM0

```

#### Trace du premier succès (Sortie réelle du Mac Mini) :

```text
T:22.5|H:55.0|S:66
T:22.8|H:55.0|S:67
T:23.2|H:55.0|S:68
T:23.7|H:55.0|S:69

```

