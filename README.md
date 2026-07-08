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

## 📅 Journal de Bord — Étape 2.2 : Ingestion et Moteur Asynchrone (Java 21)  

Validation de la lecture continue, non-bloquante et résiliente du flux matériel depuis un service d'arrière-plan Spring Boot exploitant le **Projet Loom** (Virtual Threads).  

---

### 🏗️ Modélisation du Domaine (`Record` Java 21)
Encapsulation stricte et immuable de la télémétrie matérielle :  
```java
public record PlantTelemetry(
    float temperature,
    float airHumidity,
    int soilMoisture,
    LocalDateTime timestamp
) {}
```

⚙️ Logique d'Écoute (ArduinoListenerService)

    Auto-détection du Périphérique : Le service scanne dynamiquement les descripteurs matériels Linux pour lier le premier binaire contenant la signature ACM ou USB (/dev/ttyACM*).

    Isolation Asynchrone : L'écoute du flux d'Entrées/Sorties (I/O) étant bloquante, elle est déléguée à un thread virtuel dédié (Thread.ofVirtual()) afin de préserver l'intégrité des threads majeurs du serveur web Tomcat.

    Defensive Parsing : Implémentation d'un bloc try/catch silencieux pour absorber les trames corrompues ou incomplètes lors des phases de branchement à chaud (Hot-Plug).

🧪 Trace de Rendu JVM (Preuve de Fonctionnement)
```Plaintext

2026-06-29T19:21:12.345 INFO --- [main]            : 🔌 Port cible identifié : /dev/ttyACM0
2026-06-29T19:21:12.346 INFO --- [main]            : ✅ Liaison série établie ! Lancement du Virtual Thread Loom...
2026-06-29T19:21:15.996 INFO --- [arduino-vthread] : 🌿 [Télémétrie Reçue] -> PlantTelemetry[temperature=22.5, airHumidity=55.0, soilMoisture=66]
2026-06-29T19:21:17.999 INFO --- [arduino-vthread] : 🌿 [Télémétrie Reçue] -> PlantTelemetry[temperature=22.8, airHumidity=55.0, soilMoisture=67]
2026-06-29T19:21:19.998 INFO --- [arduino-vthread] : 🌿 [Télémétrie Reçue] -> PlantTelemetry[temperature=23.2, airHumidity=55.0, soilMoisture=68]
```

Note technique : On observe un intervalle d'exécution strict de 2000ms (+/- 3ms de latence noyau) sur le thread [arduino-vthread].  

## 📅 Journal de Bord — Étape 3 : Persistance PostgreSQL et Ouverture Wi-Fi (ESP32)

L'objectif de cette semaine était de consolider les données en base 
(via PostgreSQL sous Docker) et de préparer la transition du projet 
vers une architecture IoT sans-fil (Wi-Fi via ESP32), tout en conservant 
la rétrocompatibilité avec la sonde filaire USB actuelle.  

---

### 🛠️ Infrastructure : Docker & PostgreSQL  

Pour éviter d'installer des services lourds sur l'OS, 
la base de données est isolée dans un conteneur :

```bash
# Déploiement de PostgreSQL via Docker
docker run --name biosphere-db \
  -e POSTGRES_USER=greg \
  -e POSTGRES_PASSWORD=secret \
  -e POSTGRES_DB=biospheredb \
  -p 5432:5432 \
  -d postgres:15-alpine
```
Pour avoir un volume sur le disque physique et garder les données même si l'image est supprimer.  
1. On arrête le conteneur actuel
```bash
docker stop biosphere-db
```

2. On le supprime (⚠️ Cela va effacer tes données de test actuelles, mais c'est le moment de le faire !)
```bash
docker rm biosphere-db
```

3. On crée un volume virtuel géré par Linux  
```bash
docker volume create biosphere_pgdata
```

4. On relance la commande magique EN AJOUTANT le volume (-v)
```bash
docker run --name biosphere-db \
-e POSTGRES_USER=greg \
-e POSTGRES_PASSWORD=secret \
-e POSTGRES_DB=biospheredb \
-p 5432:5432 \
-v biosphere_pgdata:/var/lib/postgresql/data \
-d postgres:15-alpine
```


#### 🏗️ Architecture Logicielle : Séparation des Responsabilités (Clean Architecture)  

Pour anticiper l'arrivée de données sous différents formats (Série vs JSON), 
le projet adopte une séparation stricte des modèles de données :  

    Couche Métier (Domaine) : PlantTelemetry (Record). C'est l'objet universel et agnostique qui circule au cœur de l'application.

    Couche Infrastructure (DAO) : TelemetryEntity (@Entity JPA). Représentation miroir de la table PostgreSQL.

    Couche Présentation (DTO) : TelemetryPayload (Record). Représentation du payload JSON attendu de l'ESP32.

#### ⚙️ Implémentation de la Persistance (JPA)
1. Entité et Repository  

Création de l'interface TelemetryRepository (héritant de JpaRepository) et 
de l'entité TelemetryEntity pour déléguer la génération SQL à Hibernate.  
2. Service d'Ingestion Centralisé  

Création du TelemetryIngestService. Son rôle est de :  

    Recevoir un objet métier pur (PlantTelemetry).

    Le convertir en objet d'infrastructure (TelemetryEntity).

    Y apposer un tag d'origine (USB ou WIFI).

    Déclencher la sauvegarde via le Repository.

#### 📡 Implémentation du Contrôleur REST (Préparation ESP32)

Ouverture d'un endpoint POST /api/telemetry pour ingérer les requêtes JSON en provenance 
du futur microcontrôleur ESP32 :
```Java
@PostMapping("/telemetry")
public ResponseEntity<String> receiveWifiTelemetry(@RequestBody TelemetryPayload payload) {
    // 1. Conversion du DTO réseau en objet Métier
    PlantTelemetry telemetry = new PlantTelemetry(
        payload.temperature(), 
        payload.humidity(), 
        payload.soil(), 
        LocalDateTime.now()
    );
    
    // 2. Délégation au service centralisé (Tag WIFI)
    ingestService.ingest(telemetry, "WIFI");
    
    return ResponseEntity.ok("OK");
}
```

#### 🧪 Validation et Tests (Preuve de Fonctionnement)

Test de charge hybride : Injection de données simulées par API (via cURL) tout en maintenant l'Arduino connecté.

Extrait des logs JVM démontrant la concurrence parfaite :
Plaintext

2026-07-04T16:49:34.801 INFO --- [arduino-vthread] : 💾 [Sauvegarde BDD] -> Origine: USB  | Temp: 23.2°C
2026-07-04T16:49:35.833 INFO --- [omcat-handler-0] : 💾 [Sauvegarde BDD] -> Origine: WIFI | Temp: 25.4°C
2026-07-04T16:49:36.809 INFO --- [arduino-vthread] : 💾 [Sauvegarde BDD] -> Origine: USB  | Temp: 23.7°C
2026-07-04T16:49:41.407 INFO --- [omcat-handler-1] : 💾 [Sauvegarde BDD] -> Origine: WIFI | Temp: 26.4°C

Note technique : Le thread virtuel [arduino-vthread] (I/O matériel) et 
le pool de threads Tomcat [omcat-handler-X] (Requêtes HTTP) coexistent et 
écrivent dans la base de données sans conflit (gestion des locks par HikariCP).

Pour vérifier les données en BDD :  
1. Comment vérifier l'état du conteneur (Niveau Docker)Pour savoir 
si ton serveur de base de données tourne, ouvre ton terminal Linux et tape :
```Bash
# Voir les conteneurs actuellement en cours d'exécution
   docker ps

# Voir TOUS les conteneurs (même ceux qui sont éteints)
docker ps -a
```

Si tu as besoin de voir les erreurs internes de PostgreSQL (par exemple si la base refuse de démarrer) :
```Bash
docker logs biosphere-db
```

2. Comment vérifier l'état des données (Niveau SQL)
Tu n'as pas besoin d'installer un gros logiciel client SQL sur ton Ubuntu. 
Tu peux demander à Docker d'exécuter la commande psql (le terminal de PostgreSQL) directement à l'intérieur de ton conteneur :
```Bash
docker exec -it biosphere-db psql -U greg -d biospheredb
```
(Ton terminal va changer et afficher biospheredb=# : tu es maintenant connecté à la base !)  
Essaie ces commandes SQL :  
```
\dt -> Affiche la liste des tables (tu vas voir telemetry_history).  

SELECT * FROM telemetry_history ORDER BY timestamp DESC LIMIT 5; -> Affiche les 5 dernières lignes ingérées.
  
\q -> Pour quitter et revenir à ton terminal Linux.  
```

(💡 Astuce Pro : Puisque tu utilises IntelliJ, tu peux aussi cliquer sur l'onglet Database à droite de ton écran, 
ajouter une source de données "PostgreSQL", mettre localhost, le port 5432, user greg, pass secret, db biospheredb. 
Tu auras une interface graphique pour explorer tes tables !)  

## 📅 Journal de Bord — Étape 3 (Fin) : Actionneurs et Flux Bidirectionnel (Downlink)

Mise en place de la capacité du serveur à interagir avec le monde physique (contrôle d'actionneurs) via l'exposition 
d'une API REST, tout en gérant l'accès concurrent au port série matériel.

---

### ⚙️ Implémentation Backend (Java / Spring Boot)

#### 1. Canal d'Écriture Matériel (`ArduinoListenerService`)
Ajout de la capacité d'écriture non-bloquante vers le microcontrôleur via l'implémentation d'une méthode de transmission 
binaire (`OutputStream`) ajoutant automatiquement le caractère de fin de ligne `\n` requis par le parseur C++.

#### 2. Contrôleur REST (`ActuatorController`)
Exposition du point d'entrée réseau permettant à une interface frontend de déclencher une action physique :
* **Route :** `POST /api/actuators/led?state={true|false}`
* **Routage :** Traduction du booléen réseau en commande matérielle brute (`CMD:LED:ON` ou `CMD:LED:OFF`).

---

### 🛠️ Implémentation Embarquée (C++ / Arduino)

Mise à jour du firmware (`biosphere_firmware.ino`) pour intégrer une écoute asynchrone :
* Utilisation stricte de `Serial.available() > 0` pour éviter de bloquer la boucle principale (`loop`).
* Extraction de la commande via `Serial.readStringUntil('\n')` et nettoyage du buffer.
* Bascule d'état matérielle sur la broche numérique intégrée (`LED_BUILTIN`).

---

### ⚠️ Résolution de Problème : Le Verrou Matériel (Hardware Lock)

**Symptôme :** Échec de la commande `arduino-cli upload` avec l'erreur "Permission denied" ou "Port busy" sur `/dev/ttyACM0`.
**Analyse technique :** Sous Linux, l'accès à un descripteur de périphérique (`/dev/tty*`) est généralement exclusif. 
Le service Spring Boot (via la librairie JNI `jSerialComm`) maintient un verrou (lock) sur le fichier matériel tant que le serveur est actif.
**Solution (Procédure de Flashage) :**
1. Interruption du daemon Java (`Ctrl+C` sur le serveur Spring Boot).
2. Téléversement du nouveau firmware via `arduino-cli`.
3. Redémarrage du serveur Java.

---
### 🚀 Le Test de Vérité (Full-Stack Hardware)

    Regarde physiquement ta carte Arduino. La LED "L" (près du pin 13) doit être éteinte.

    Ouvre un terminal Linux et envoie une requête POST à ton API locale pour allumer la LED :

```Bash
curl -X POST "http://localhost:8080/api/actuators/led?state=true"
```
        Dans la console IntelliJ, tu verras les logs du Controller et du Service Série.

        Sur ton bureau, la LED de l'Arduino va s'allumer instantanément !

Pour l'éteindre :  

```Bash
curl -X POST "http://localhost:8080/api/actuators/led?state=false"
```


---

### 🧪 Preuve de Fonctionnement (Trace Console)

Entrelacement réussi des requêtes HTTP synchrones et de la télémétrie asynchrone :

```text
2026-07-08T18:25:54.249 INFO [arduino-vthread] : 💾 [Sauvegarde BDD] -> Origine: USB | Temp: 22.8°C
2026-07-08T18:25:55.856 INFO [omcat-handler-0] : 🕹️ [API REST] Demande de modification d'état reçue : LED -> ON
2026-07-08T18:25:55.857 INFO [omcat-handler-0] : 📤 [Commande envoyée au matériel] -> CMD:LED:ON
2026-07-08T18:25:56.256 INFO [arduino-vthread] : 💾 [Sauvegarde BDD] -> Origine: USB | Temp: 23.1°C
[...]
2026-07-08T18:26:08.887 INFO [omcat-handler-1] : 🕹️ [API REST] Demande de modification d'état reçue : LED -> OFF
2026-07-08T18:26:08.888 INFO [omcat-handler-1] : 📤 [Commande envoyée au matériel] -> CMD:LED:OFF
2026-07-08T18:26:10.264 INFO [arduino-vthread] : 💾 [Sauvegarde BDD] -> Origine: USB | Temp: 24.5°C  
```


## 📅 Journal de Bord — Étape 4.1 : Scaffolding Angular et Sécurité CORS

Mise en place de la couche présentation (Frontend) via le framework Angular (v17+) configuré en mode Standalone Components, et résolution des politiques de sécurité inter-domaines.

---

### ⚙️ Déverrouillage Backend (Spring Boot)

Les navigateurs web bloquent par défaut les requêtes XHR/Fetch provenant d'un port différent de celui du serveur (Politique *Same-Origin*). 
Création d'une configuration globale `WebMvcConfigurer` pour autoriser le trafic entrant depuis le serveur de développement Angular :

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
```  

### Installer Node.js et l'Angular CLI (Sur Ubuntu)
Le standard industriel pour installer Node.js sous Linux est d'utiliser nvm (Node Version Manager). 
Cela évite tous les problèmes de permissions sudo avec les paquets globaux (npm -g).Ouvre ton terminal Ubuntu et exécute ces commandes une par une :

1. Installer NVM  
```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
```  

2. Recharger le terminal (pour que la commande 'nvm' soit reconnue)
```bash
source ~/.bashrc
```  

3. Installer la version LTS actuelle de Node.js (ex: Node 20/22)
```bash
nvm install --lts
```  

4. Installer le CLI officiel Angular mondialement
```bash
npm install -g @angular/cli
```  

Vérification : Tape ng version. Tu devrais voir un gros logo Angular rouge en ASCII.  

### Scaffolding du Projet Angular
Place-toi à la racine de ton dépôt Git (là où tu as le dossier backend et firmware) et lance le générateur :

On génère le squelette frontend (Angular CLI va tout configurer)  
```bash
ng new frontend
```

Le CLI va te poser quelques questions, réponds avec ces choix :  
Which stylesheet format would you like to use? -> CSS (ou SCSS si tu es à l'aise).
Do you want to enable Server-Side Rendering (SSR) and Static Site Generation (SSG/Prerendering)? -> N (Pour un dashboard de serre en réseau local, on veut une pure Single Page Application cliente).

### L'Allumage du Moteur  
Une fois que le CLI a terminé de télécharger les paquets node_modules (ça peut prendre une ou deux minutes), rentre dans le dossier et démarre le serveur de développement :
```Bash
cd frontend
ng serve
```  

Le terminal va compiler l'application en mémoire.

### 🛠️ Infrastructure Frontend & Résolution d'Anomalie Linux

Anomalie rencontrée : Échec de la génération du projet (ng new) avec l'erreur EPERM: operation not permitted, symlink.  

Diagnostic système : L'Angular CLI (via npm) nécessite la création de liens symboliques Unix pour l'arborescence node_modules/.bin. Le disque de travail initial étant formaté avec un système de fichiers non-natif (exFAT/NTFS), le noyau Linux a bloqué l'opération système.  

Résolution : 
1. Migration de l'espace de travail vers un disque monté en ext4 (/run/media/.../EXTGREG).
2. Réattribution des droits de propriété stricts au niveau de l'OS (sudo chown -R $USER:$USER ...).
3. Initialisation réussie de l'architecture Single Page Application (SPA).

## 📅 Journal de Bord — Étape 4.2 : Le Pont HTTP (PostgreSQL -> Spring Boot -> Angular)

Établissement de la communication bidirectionnelle entre la Single Page Application (Angular 17+) et l'API REST (Spring Boot), avec récupération de l'historique de télémétrie.

---

### ⚙️ Évolution du Backend (API de Lecture)

Pour exposer les données stockées en base au frontend, la couche de persistance et le contrôleur ont été mis à jour :

1.  **Repository (Spring Data JPA) :** Ajout d'une méthode de requête dérivée pour extraire le dernier lot de données sans écrire de SQL manuel :
    ```java
    List<TelemetryEntity> findTop15ByOrderByTimestampDesc();
    ```
2.  **Contrôleur REST :** Création du point d'entrée `GET /api/telemetry` exposant l'historique au format JSON.

---

### 🛠️ Implémentation Frontend (Angular 17+)

Adoption des standards modernes du framework (zéro `NgModule`) pour l'architecture des requêtes :

1.  **Activation Globale :** Injection du client HTTP au niveau du bootstrap de l'application via `provideHttpClient()` dans `app.config.ts`.
2.  **Service Métier (`BiosphereService`) :** * Définition de l'interface stricte `Telemetry` (miroir du DTO Java).
    * Utilisation de la nouvelle fonction `inject(HttpClient)` remplaçant l'injection par constructeur.
    * Implémentation des méthodes réactives (retournant des `Observable`) pour la lecture (`GET`) et l'écriture (`POST` vers l'actionneur).

---

### 🧪 Preuve de Fonctionnement (End-to-End)

**Validation de la chaîne complète (Capteur -> BDD -> Navigateur) :**
Appel du service dès l'initialisation du composant racine (`ngOnInit`). La console du navigateur (Outils de développement) confirme la réception et le parsing correct du payload JSON provenant de PostgreSQL :
`✅ Données reçues de PostgreSQL : Array(15) [ Object { temperature: 19, humidity: 55, ... }, ... ]`