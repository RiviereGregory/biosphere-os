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

## 📅 Journal de Bord — Étape 2 : Le Daemon d'Ingestion (Backend V1)  

L'objectif de la **Semaine 2** est de substituer la lecture manuelle du port série (`cat /dev/ttyACM0`) par un service d'arrière-plan autonome et résilient développé en **Java 21 / Spring Boot 3**.  

Le défi architectural principal consiste à lire un flux d'Entrées/Sorties (I/O) matériellement bloquant sans gaspiller de ressources système, en déléguant l'écoute du noyau Linux à un **Virtual Thread** (Projet Loom).  

---

## 🛠️ Architecture Logicielle & Choix Techniques  

* **Langage & Framework :** Java 21 LTS / Spring Boot 3.3+ (Gestion de projet : *Maven Wrapper*)  
* **Driver Matériel :** `com.fazecast:jSerialComm` v2.10+ (Pont JNI natif vers le noyau Linux)  
* **Modèle de Concurrence :** Virtual Threads activés globalement (`spring.threads.virtual.enabled=true`)  

```
[ ARDUINO UNO ] ─── (Flux UART : /dev/ttyACM0) ───> [ NOYAU LINUX / UBUNTU ]
│
(Driver JNI jSerialComm)
▼
[ SPRING BOOT (Java 21) ]
└── Virtual Thread (Loom)
└── TelemetryParserService
```

---

## ⚙️ Procédure d'Initialisation (Environnement Vierge Linux)

### Prérequis Système (Ubuntu)
Le script `./mvnw` embarque son propre moteur Maven, mais nécessite impérativement la présence du compilateur Java 21 sur le système hôte.

```bash
# 1. Mise à jour des dépôts système
sudo apt update

# 2. Installation du Kit de Développement Java 21 (JDK complet)
sudo apt install openjdk-21-jdk -y

# 3. Vérification de la liaison
java -version
```

Génération du Projet (CLI / cURL)  

Depuis la racine du dépôt Git (~/biosphere-os/) :  
```Bash

mkdir -p backend && cd backend

# Téléchargement du squelette Spring Boot via l'API Spring Initializr
curl [https://start.spring.io/starter.tgz](https://start.spring.io/starter.tgz) \
  -d dependencies=web \
  -d type=maven-project \
  -d language=java \
  -d baseDir=. \
  -d groupId=com.biosphere \
  -d artifactId=biosphere-backend \
  -d name=biosphere-backend \
  -d packageName=com.biosphere.backend \
  -d javaVersion=21 | tar -xzvf -

```

🔧 Configuration des Dépendances & Propriétés  
1. Ajout du pilote série (pom.xml)  

Intégration de la librairie industrielle au sein de la balise <dependencies> :  
```XML

<dependency>
    <groupId>com.fazecast</groupId>
    <artifactId>jSerialComm</artifactId>
    <version>2.10.4</version>
</dependency>
```

2. Configuration du Serveur (application.properties)

Activation des threads légers managés par la JVM :  
```Properties

spring.application.name=biosphere-backend
spring.threads.virtual.enabled=true
server.port=8080
```

⚠️ Résolution des Problèmes Système (Troubleshooting Linux)  

    Perte des droits d'exécution sur le Wrapper Maven :  
```Bash

chmod +x mvnw

```

    Contournement de la sécurité noexec (Spécificité disques externes/secondaires) :  
    Si le projet réside sur une partition montée dans /run/media/..., le noyau Ubuntu peut bloquer l'exécution directe de binaires. La compilation doit être déléguée à l'interpréteur système :  
```Bash

bash mvnw clean compile
```

🧪 Statut de Validation

    Compilation initiale : OK (BUILD SUCCESS généré via bash mvnw)

    Téléchargement des dépendances JNI Linux : Validé dans le cache local (~/.m2/repository)
