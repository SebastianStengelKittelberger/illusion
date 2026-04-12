# PRD: Generic Adapter Wizard mit KI-Unterstützung

**Status**: Entwurf  
**Erstellt**: 29. März 2026  
**Priorität**: Hoch — strategisches Differenzierungsmerkmal

---

## Problem

Jeder neue Illusion-Kunde benötigt heute einen **kundenspezifischen Adapter** (z.B. `bosch.adapter`), der in Java entwickelt werden muss. Das bedeutet:

- Onboarding dauert Wochen statt Stunden
- Jeder Kunde ist ein Individualprojekt — kein Skalierungseffekt
- Kunden können nicht selbst onboarden
- Das System ist ein Dienstleistungsprodukt, kein Self-Service-Produkt

**Ziel**: Ein neuer Kunde mit einer REST-API, JDBC-Datenbank, CSV- oder XML-Datei kann sein Produktkatalog **selbst in Illusion integrieren** — ohne eine Zeile Code zu schreiben.

---

## Zielgruppe

| Persona | Beschreibung |
|---|---|
| **Technischer Redakteur** | Versteht Dateistrukturen und APIs, aber schreibt keinen Java-Code |
| **IT-Projektleiter** | Verantwortet die Integration, möchte Fortschritt sehen |
| **Illusion-Partner** | Dienstleister der Illusion bei Kunden einführt, braucht schnelles Onboarding |

---

## Lösung: Generic Adapter Wizard

Ein geführter Wizard in **Summerlight** (`/adapter`), der in 5 Schritten eine vollständig konfigurierte Datenquellenanbindung erstellt. KI unterstützt bei der Feldanalyse und Transformationsgenerierung — der Mensch entscheidet, die KI macht Vorschläge.

---

## User Stories

### Muss (MVP)

**US-01 — Datenquelle verbinden**  
Als Nutzer möchte ich eine Datenquelle (REST API, CSV, XML) über ein Formular angeben, damit Illusion die Rohdaten laden kann.

**US-02 — Struktur analysieren**  
Als Nutzer möchte ich, dass Illusion nach dem Verbinden automatisch Beispieldaten lädt und mir die erkannte Feldstruktur zeigt.

**US-03 — KI-Feldmapping**  
Als Nutzer möchte ich KI-gestützte Vorschläge erhalten, welches Quellfeld welchem UKey entspricht, damit ich nicht jeden Feldnamen manuell zuordnen muss.

**US-04 — Mapping bestätigen/korrigieren**  
Als Nutzer möchte ich jeden Vorschlag akzeptieren, ablehnen oder manuell anpassen, bevor das Mapping gespeichert wird.

**US-05 — Adapter speichern & ausführen**  
Als Nutzer möchte ich den konfigurierten Adapter speichern und einen ersten Indexierungslauf starten, um sofort Ergebnisse zu sehen.

### Sollte (Post-MVP)

**US-06 — KI-Transformation**  
Als Nutzer möchte ich natürlichsprachlich beschreiben, wie ein Feldwert transformiert werden soll (z.B. „Gramm in Kilogramm"), und eine automatisch generierte Transformation erhalten.

**US-07 — Scheduler**  
Als Nutzer möchte ich einstellen, wie oft der Adapter die Datenquelle neu einliest (Cron-Expression oder Webhook-Trigger).

**US-08 — JDBC-Datenbank**  
Als Nutzer möchte ich eine SQL-Datenbank als Datenquelle angeben und eine SQL-Abfrage definieren, deren Ergebnis als Produktdaten indexiert wird.

**US-09 — Adapter-Vorlagen**  
Als Nutzer möchte ich aus einer Bibliothek bekannter Adapter-Vorlagen (z.B. „Shopify", „SAP", „WooCommerce") wählen, damit ich nicht von Null beginnen muss.

---

## Wizard-Schritte (UX-Flow)

```
Schritt 1: Datenquelle wählen
    └── REST API / CSV-Upload / XML-Upload / JDBC (Post-MVP)

Schritt 2: Verbindung konfigurieren & testen
    └── URL, Auth, Pfad zu Produktliste, Sample laden

Schritt 3: Felder analysieren (automatisch)
    └── Illusion parst Sample → zeigt erkannte Felder + Beispielwerte

Schritt 4: KI-Mapping Review
    └── Vorschläge anzeigen, bestätigen/anpassen, neue UKeys anlegen

Schritt 5: Speichern & ersten Lauf starten
    └── AdapterConfig in ES speichern → Indexierung starten → Ergebnis zeigen
```

---

## KI-Integration

### Ansatz

Der KI-Aufruf findet **serverseitig in Illusion** statt — Summerlight sendet die Rohdaten an einen neuen Illusion-Endpoint, der die KI anfragt und strukturierte Vorschläge zurückgibt.

```
Summerlight → POST /adapter-wizard/analyze
                    { fields: [{ name, sampleValues[] }] }
              ← [{ sourceField, suggestedUkey, mappingType, confidence }]
```

### Prompt-Design

```
System-Prompt:
  Du bist ein Produktdaten-Experte. Analysiere Feldnamen und Beispielwerte
  eines Produktkatalogs. Schlage für jedes Feld einen UKey (Kurzbezeichner
  in GROSSBUCHSTABEN), einen MappingType (TEXT, IMAGE, COMPLEX) und einen
  TargetFieldType (STRING, IMAGE, LIST) vor.
  Antworte ausschließlich als JSON-Array ohne Erklärung.

User-Prompt (generiert aus Beispieldaten):
  Felder:
  - "product_voltage_v": ["220", "230", "110"]
  - "image_url_main": ["https://cdn.../abc.jpg"]
  - "weight_grams": ["1400", "850", "2100"]
  - "long_description_de": ["Leistungsstarker..."]
  - "ean_code": ["4003246892818"]

Erwartete Antwort:
  [
    { "sourceField": "product_voltage_v",  "ukey": "VOLTAGE",     "mappingType": "TEXT",  "targetFieldType": "STRING", "confidence": 0.95 },
    { "sourceField": "image_url_main",     "ukey": "PRODIMG",     "mappingType": "IMAGE", "targetFieldType": "IMAGE",  "confidence": 0.98 },
    { "sourceField": "weight_grams",       "ukey": "WEIGHT",      "mappingType": "TEXT",  "targetFieldType": "STRING", "confidence": 0.82 },
    { "sourceField": "long_description_de","ukey": "DESCRIPTION", "mappingType": "TEXT",  "targetFieldType": "STRING", "confidence": 0.91 },
    { "sourceField": "ean_code",           "ukey": "EAN",         "mappingType": "TEXT",  "targetFieldType": "STRING", "confidence": 0.96 }
  ]
```

### KI-Provider (konfigurierbar)

```yaml
# application.yaml
illusion:
  ai:
    provider: ollama          # ollama | openai | azure-openai
    model: llama3.2           # llama3.2 | gpt-4o | gpt-4o-mini
    baseUrl: http://localhost:11434
    apiKey: ${OPENAI_API_KEY:}
```

| Provider | Einsatz | Datenschutz |
|---|---|---|
| **Ollama (lokal)** | On-Premise, Standard | ✅ Daten verlassen nicht den Server |
| **OpenAI** | Cloud, beste Qualität | ⚠️ Daten gehen an OpenAI |
| **Azure OpenAI** | Enterprise Cloud | ✅ DSGVO-konform, EU-Rechenzentrum |

---

## Datenmodell

### AdapterConfig (neu in Illusion)

```java
public class AdapterConfig {
  private String id;                        // UUID
  private String name;                      // "Bosch Katalog DE"
  private String country;
  private String language;
  private ConnectorConfig connector;        // Verbindungsdetails
  private List<FieldMapping> fieldMappings; // Quellfeld → UKey
  private ScheduleConfig schedule;          // Cron / Webhook
  private Instant lastRun;
  private String lastRunStatus;             // SUCCESS, FAILED, RUNNING
}

public class ConnectorConfig {
  private ConnectorType type;               // REST_API, CSV, XML, JDBC
  private String url;
  private String authType;                  // NONE, BEARER, BASIC, API_KEY
  private String authValue;
  private String itemsPath;                 // JSONPath zur Produktliste: "$.products[*]"
  private String skuField;                  // Welches Feld ist die SKU?
}

public class FieldMapping {
  private String sourcePath;               // JSONPath / CSV-Spalte
  private String targetUkey;               // UKey in Illusion
  private String transformExpression;      // optional: Java-Snippet
  private boolean aiSuggested;
  private double aiConfidence;
}
```

### ES-Index

`illusion-adapters` — ein Dokument pro AdapterConfig (wie `illusion-mapping-config`)

---

## Neue Endpoints (Illusion)

| Methode | Pfad | Beschreibung |
|---|---|---|
| `GET` | `/{country}/{language}/adapters` | Alle Adapter auflisten |
| `POST` | `/{country}/{language}/adapters` | Neuen Adapter speichern |
| `PUT` | `/{country}/{language}/adapters/{id}` | Adapter aktualisieren |
| `DELETE` | `/{country}/{language}/adapters/{id}` | Adapter löschen |
| `POST` | `/adapter-wizard/test-connection` | Verbindung testen + Sample laden |
| `POST` | `/adapter-wizard/analyze` | KI-Feldanalyse (gibt Mapping-Vorschläge zurück) |
| `POST` | `/{country}/{language}/adapters/{id}/run` | Adapter manuell ausführen |
| `GET` | `/{country}/{language}/adapters/{id}/status` | Letzter Laufstatus + Log |

---

## UI-Screens (Summerlight)

### `/adapter` — Adapter-Übersicht

- Liste aller konfigurierten Adapter
- Status: Letzter Lauf, Anzahl indexierter Produkte, Fehler
- Button: „+ Neuer Adapter" (startet Wizard)
- Manuell ausführen / Bearbeiten / Löschen

### `/adapter/wizard` — 5-Schritt-Wizard

**Schritt 1 — Typ wählen**
Kacheln: REST API / CSV hochladen / XML hochladen / JDBC (Post-MVP)

**Schritt 2 — Verbindung konfigurieren**
- REST API: URL, Auth-Typ, Auth-Value, JSONPath zu Produktliste, SKU-Feld
- CSV/XML: Datei-Upload, Encoding, Trennzeichen (CSV)
- „Verbindung testen" → zeigt 3 Beispielprodukte oder Fehler

**Schritt 3 — Feldstruktur (automatisch)**
- Erkannte Felder als Tabelle: Feldname | Typ (String/Number/URL) | Beispielwert
- „KI-Analyse starten" Button

**Schritt 4 — Mapping Review**

Tabelle mit KI-Vorschlägen, jede Zeile editierbar:

| Quellfeld | Beispielwert | → UKey | Typ | Confidence | |
|---|---|---|---|---|---|
| product_voltage_v | "220" | VOLTAGE | TEXT | 🟢 95% | ✓ / ✏ / 🗑 |
| image_url_main | "https://..." | PRODIMG | IMAGE | 🟢 98% | ✓ / ✏ / 🗑 |
| weight_grams | "1400" | WEIGHT | TEXT | 🟡 82% | ✓ / ✏ / 🗑 |
| unknown_field_x | "Q2-2024" | ??? | — | 🔴 28% | Zuordnen / Ignorieren |

- Niedrige Confidence (< 60%): rot, Pflichtaktion vor Weiter
- Neue UKeys können direkt angelegt werden (öffnet QuickMapModal)

**Schritt 5 — Zusammenfassung & Start**
- Name vergeben
- Zeitplan: Manuell / täglich / stündlich / Webhook-URL
- „Speichern & Indexierung starten"
- Fortschrittsanzeige: X von Y Produkten indexiert

---

## Abgrenzung

| Im Scope (MVP) | Nicht im Scope (MVP) |
|---|---|
| REST API als Quelle | JDBC / SQL-Datenbanken |
| CSV / XML Upload | Excel (.xlsx) |
| KI-Feld-Mapping | KI-Transformation-Generator |
| Manueller Start | Automatischer Scheduler |
| Ollama lokal | Azure OpenAI |
| Flache JSON-Strukturen | Tief verschachtelte Objekte |

---

## Erfolgskriterien

| KPI | Ziel |
|---|---|
| Onboarding-Zeit für neuen Kunden | < 2 Stunden (heute: Wochen) |
| Mapping-Vorschläge mit Confidence ≥ 80% | ≥ 70% der Felder |
| Manuelle Korrekturen pro Adapter | ≤ 20% der Felder |
| Kunden können selbst onboarden (ohne Illusion-Entwickler) | Ja |

---

## Technische Abhängigkeiten

- **Ollama** muss auf dem Server laufen (oder OpenAI API-Key konfiguriert sein)
- Illusion benötigt neuen `AdapterService` + `AdapterController`
- Summerlight bekommt neuen Bereich `/adapter` mit Wizard-Komponente
- Neuer ES-Index `illusion-adapters`
- Bestehender `bosch.adapter` bleibt als Referenzimplementierung erhalten — kann durch Generic Adapter abgelöst werden

---

## Offene Fragen

1. **Datenschutz**: Welche Kundendaten dürfen die KI-API erreichen? → Ollama als Pflicht-Default
2. **Komplexe Transformationen**: Was passiert wenn Java-Code nötig ist? → Fallback auf manuellen JAVA_CODE-Mapping-Typ
3. **Fehlerbehandlung bei Adapter-Lauf**: Partial-Failures (1 von 1000 Produkten schlägt fehl) — Rollback oder Weitermachen?
4. **Versionierung**: Soll eine Änderung am Adapter automatisch die MapConfig versionieren?
