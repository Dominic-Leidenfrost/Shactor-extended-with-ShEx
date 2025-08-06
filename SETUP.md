# SHACTOR Setup Guide

Schnelle Anleitung zum Einrichten des SHACTOR-Projekts auf deinem lokalen System.

## Was du brauchst

- **Java 11+** und **Maven 3.6+**
- **GraphDB** (für SPARQL-Datenbank)
- **Git**

## Setup in 5 Schritten

### 1. Projekt klonen
```bash
git clone <repository-url>
cd demo-shactor
```

### 2. Konfiguration einrichten

**Einfachste Methode:**
```bash
# Beispiel-Konfiguration kopieren
cp application.properties.example src/main/resources/application.properties
```

Dann die Datei `src/main/resources/application.properties` öffnen und deine lokalen Pfade eintragen:

```properties
# Deine Dataset-Pfade hier eintragen
dataset.lubm.mini.path=/dein/pfad/zu/lubm-mini.nt
dataset.dbpedia.path=/dein/pfad/zu/dbpedia_ml.nt

# SPARQL Endpoint (normalerweise localhost)
sparql.endpoint.default.url=http://localhost:7200/
sparql.endpoint.default.repository=LUBM-ScaleFactor-1
```

### 3. Datasets besorgen

Erstelle einen `datasets/` Ordner und besorge dir die Dateien:
- `lubm-mini.nt` (zum Testen)
- `dbpedia_ml.nt`, `lubm.n3`, `yago.n3` (für vollständige Funktionalität)

**Hinweis:** Frage deinen Betreuer nach den Dataset-Dateien.

### 4. GraphDB einrichten

1. [GraphDB herunterladen](https://www.ontotext.com/products/graphdb/) und installieren
2. GraphDB starten → `http://localhost:7200`
3. Repositories erstellen:
   - `LUBM-ScaleFactor-1`
   - `DBPEDIA_ML`
   - `LUBM`
   - `Yago_EngWiki`
4. Datasets in die entsprechenden Repositories importieren

### 5. Projekt starten

```bash
mvn clean compile
mvn spring-boot:run
```

→ Öffne `http://localhost:8080`

## Häufige Probleme

| Problem | Lösung |
|---------|--------|
| "application.properties not found" | Schritt 2 wiederholen - Datei kopieren |
| "Dataset file not found" | Pfade in der Konfiguration prüfen |
| "Cannot connect to SPARQL endpoint" | GraphDB läuft? Repository-Namen korrekt? |
| Build-Fehler | `mvn clean install` ausführen |

## Konfiguration im Detail

Die wichtigsten Einstellungen in `application.properties`:

```properties
# Dataset-Pfade (anpassen!)
dataset.lubm.mini.path=${LUBM_MINI_PATH:./datasets/lubm-mini.nt}
dataset.dbpedia.path=${DBPEDIA_PATH:./datasets/dbpedia_ml.nt}

# SPARQL Endpoints
sparql.endpoint.default.url=${SPARQL_ENDPOINT_URL:http://localhost:7200/}
sparql.endpoint.remote.url=${SPARQL_REMOTE_URL:http://10.92.0.34:7200/}

# Repository-Namen (müssen mit GraphDB übereinstimmen)
repository.lubm.mini=${REPO_LUBM_MINI:LUBM-ScaleFactor-1}
repository.dbpedia=${REPO_DBPEDIA:DBPEDIA_ML}
```

## Alternative: Umgebungsvariablen

Statt die `application.properties` zu bearbeiten, kannst du auch Umgebungsvariablen setzen:

```bash
export LUBM_MINI_PATH="/dein/pfad/zu/lubm-mini.nt"
export SPARQL_ENDPOINT_URL="http://localhost:7200/"
export SPARQL_REPOSITORY="LUBM-ScaleFactor-1"
```

## Warum diese Konfiguration?

**Vorher:** Jeder Entwickler musste hardcodierte Pfade im Quellcode ändern  
**Jetzt:** Nur eine Konfigurationsdatei anpassen → viel einfacher!

## Erste Schritte nach dem Setup

1. Mit LUBM-Mini dataset testen
2. SPARQL-Verbindung prüfen
3. Shape-Extraktion ausprobieren

Bei Problemen: Logs prüfen oder Team kontaktieren! 🚀