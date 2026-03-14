# Generic Adapter – Konzept & Architektur

> Ziel: Neue Datenquellen ohne Entwicklungsaufwand in illusion einbinden –
> direkt im Wizard, in unter 10 Minuten.

---

## Das Problem

Aktuell benötigt jede neue Datenquelle einen eigenen Adapter:

```
Kundendaten → [eigener Adapter, extra Entwicklung] → illusion
```

Das ist eine Einstiegshürde die den 10-Minuten-Wizard verhindert.

Dazu kommt: Kundendaten haben fast nie die gleiche Struktur wie illusion sie erwartet:

```
Kundendatenbank:                illusion erwartet:
{                               Product {
  "art_nr": "0601015200",   →     productMetaData.artNo
  "bez": "Bohrmaschine",    →     productMetaData.name
  "volt": 18,               →     skuAttributes[VOLTAGE].references["TEXT"]
  "gewicht_g": 1900,        →     skuAttributes[WEIGHT].references["TEXT"]
  "aktiv": true             →     ?
}
```

Der Generic Adapter muss also zwei Probleme lösen:
1. **Verbindung** – Daten aus beliebigen Quellen abrufen
2. **Normalisierung** – fremde Strukturen in illusions internes Format überführen

---

## Die Lösung: Zwei-Ebenen-Architektur

```
Ebene 1: Generic Adapter (Wizard)
─────────────────────────────────────────────────────
Quelldaten → illusion-internes Format (Normalisierung)
"bez"       → productMetaData.name
"volt"      → skuAttribute mit UKEY "VOLTAGE"
"gewicht_g" → skuAttribute mit UKEY "WEIGHT" (÷ 1000)

Ebene 2: illusion MapConfig (bestehend)
─────────────────────────────────────────────────────
illusion-internes Format → Ausgabe-Template
skuAttribute VOLTAGE     → "Spannung: {value} V"
skuAttribute WEIGHT      → "{value} kg"
```

Ebene 1 löst das **Struktur-Problem** (wohin gehört das Feld?).
Ebene 2 löst das **Darstellungs-Problem** (wie wird es angezeigt?).
Beide Ebenen sind getrennt konfigurierbar.

---

## Unterstützte Datenquellen

### Stufe 1: Kein Code, sofort nutzbar

**REST API**
- URL + optionaler Auth-Header (Bearer Token, Basic Auth, API Key)
- illusion ruft die API direkt ab
- Unterstützt: JSON-Arrays, paginierte Responses, verschachteltes JSON mit rootPath
- Beispiel: `GET https://erp.kunde.com/api/products`

**Relationale Datenbank (JDBC)**
- JDBC-URL + Credentials + SQL-Query
- illusion verbindet sich direkt mit Read-Only-Rechten
- Unterstützt: MySQL, PostgreSQL, MSSQL, Oracle
- Beispiel: `SELECT art_nr, bez, volt, gewicht_g FROM products WHERE aktiv = 1`

**CSV / Excel Upload**
- Datei hochladen, Trennzeichen wählen
- illusion erkennt Spalten automatisch
- Unterstützt: UTF-8, verschiedene Trennzeichen, erste Zeile als Header

**FTP / SFTP**
- Host, Pfad, Credentials
- Datei wird regelmäßig abgerufen (Cronjob konfigurierbar)
- Unterstützt: CSV, JSON, XML

### Stufe 2: Custom Adapter (Entwickler, 1-2 Tage)
Für komplexe Fälle die der Generic Adapter nicht abdeckt:
- Legacy-Systeme (SOAP, proprietäre Formate)
- Komplexe verschachtelte Datenstrukturen
- Spezielle Authentifizierungsverfahren
- Echtzeit-Daten via Websocket / Kafka

---

## Wizard-Flow im Detail

```
┌─────────────────────────────────────────────────────────┐
│  Schritt 1: Datenquelle wählen                          │
│                                                         │
│  ○ REST API      ○ Datenbank      ○ CSV      ○ FTP      │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Schritt 2: Verbindung konfigurieren (Beispiel REST)    │
│                                                         │
│  URL:   https://erp.kunde.com/api/products              │
│  Auth:  Bearer Token  [••••••••••••••••]                │
│  Pfad:  (optional) result.items                         │
│                                                         │
│  [Verbindung testen]                                    │
│  ✅ Erreichbar – 1.247 Datensätze gefunden              │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Schritt 3a: Felder erkannt (automatisch)               │
│                                                         │
│  Feld         Typ      Befüllung  Beispiel              │
│  ──────────────────────────────────────────             │
│  art_nr       String   100%       "0601015200"          │
│  bez          String   100%       "Bohrmaschine GSB"    │
│  volt         Number   98%        18                    │
│  gewicht_g    Number   91%        1900                  │
│  aktiv        Boolean  100%       true                  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Schritt 3b: Struktur-Mapping (Normalisierung)          │
│                                                         │
│  Wohin soll dieses Feld in illusion?                    │
│                                                         │
│  "bez"  (String, Beispiel: "Bohrmaschine GSB")          │
│  ○ Produktname     → productMetaData.name    ⭐          │
│  ○ SKU-Attribut    → UKEY eingeben: [      ] │
│  ○ Artikel-Nr.     → productMetaData.artNo              │
│  ○ Ignorieren                                           │
│                                                         │
│  "gewicht_g"  (Number, Beispiel: 1900)                  │
│  ○ SKU-Attribut    → UKEY: [WEIGHT]          ⭐          │
│  Transformation:                                        │
│  ○ Direkt          ○ ÷ 1000 → 1.9   ○ Text: "{value} g"│
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Schritt 4: Template wählen & Darstellungs-Mapping      │
│                                                         │
│  [Produktkatalog Standard] [Datenblatt] [Minimalistisch]│
│                                                         │
│  Template-Platzhalter    illusion-Feld                  │
│  ────────────────────    ──────────────────             │
│  [ Haupttitel      ] ←── productMetaData.name    ✅     │
│  [ Spannung        ] ←── skuAttribute VOLTAGE    ✅     │
│  [ Gewicht         ] ←── skuAttribute WEIGHT     ✅     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Schritt 5: Vorschau mit echten Daten                   │
│                                                         │
│  ┌───────────────────────────────┐                      │
│  │  Bohrmaschine GSB 18V         │                      │
│  │  Spannung: 18 V               │                      │
│  │  Gewicht: 1.9 kg              │                      │
│  └───────────────────────────────┘                      │
│                                                         │
│  [← Zurück]        [Katalog veröffentlichen →]          │
└─────────────────────────────────────────────────────────┘
```

---

## Architektur

### AdapterConfig – Verbindungskonfiguration

```java
@Entity
public class AdapterConfig {
    private String tenantId;
    private AdapterType type;        // REST, JDBC, CSV, FTP, CUSTOM
    private String sourceUrl;        // URL, JDBC-URL oder FTP-Host
    private String authType;         // BEARER, BASIC, API_KEY, NONE
    private String authCredential;   // verschlüsselt gespeichert (AES-256)
    private String rootPath;         // JSON-Pfad zur Datenliste z.B. "result.items"
    private SyncSchedule schedule;   // REALTIME, HOURLY, DAILY, MANUAL
}
```

### NormalizationConfig – Struktur-Mapping aus Wizard Schritt 3b

```java
@Entity
public class NormalizationConfig {
    private String adapterConfigId;
    private String idField;                      // "art_nr"
    private String nameField;                    // "bez"
    private String artNoField;                   // "art_nr"
    private List<AttributeMapping> attributeMappings;
}

public class AttributeMapping {
    private String sourceField;     // "volt"
    private String ukey;            // "VOLTAGE"
    private TransformType transform; // NONE, DIVIDE, MULTIPLY, TEXT_TEMPLATE, CONDITIONAL
    private String transformParam;  // z.B. "1000" bei DIVIDE oder "{value} kg" bei TEXT_TEMPLATE
}
```

### NormalizationService – Rohdaten → illusion-Format

```java
@Service
public class NormalizationService {

    public Product normalize(
        Map<String, Object> rawData,
        NormalizationConfig config
    ) {
        return new Product(
            new ProductMetaData(
                getString(rawData, config.getNameField()),
                getLong(rawData, config.getIdField()),
                getString(rawData, config.getArtNoField())
            ),
            List.of(),
            mapToAttributes(rawData, config.getAttributeMappings()),
            List.of()
        );
    }

    private List<Attribute> mapToAttributes(
        Map<String, Object> rawData,
        List<AttributeMapping> mappings
    ) {
        return mappings.stream().map(mapping -> {
            Object rawValue = rawData.get(mapping.getSourceField());
            Object transformed = transform(rawValue, mapping);
            Attribute attr = new Attribute();
            attr.setUkey(mapping.getUkey());
            attr.setReferences(Map.of(
                "TEXT", transformed,
                "BOOLEAN", false
            ));
            return attr;
        }).toList();
    }

    private Object transform(Object value, AttributeMapping mapping) {
        return switch (mapping.getTransform()) {
            case DIVIDE        -> ((Number) value).doubleValue()
                                  / Double.parseDouble(mapping.getTransformParam());
            case MULTIPLY      -> ((Number) value).doubleValue()
                                  * Double.parseDouble(mapping.getTransformParam());
            case TEXT_TEMPLATE -> mapping.getTransformParam()
                                  .replace("{value}", value.toString());
            case CONDITIONAL   -> evaluateConditional(value, mapping.getTransformParam());
            default            -> value;
        };
    }
}
```

---

## Grenzen des Generic Adapters

| Szenario | Generic Adapter | Custom Adapter |
|---|---|---|
| REST API mit JSON-Array | ✅ | |
| REST API mit verschachteltem JSON | ⚠️ mit rootPath | ✅ |
| Relationale Datenbank | ✅ | |
| Einfache Transformationen (÷, ×, Text) | ✅ | |
| Komplexe Join-Logik über mehrere Tabellen | ⚠️ eingeschränkt | ✅ |
| SOAP / XML-Webservices | ❌ | ✅ |
| Kafka / Echtzeit-Events | ❌ | ✅ |
| Proprietäre Binärformate | ❌ | ✅ |
| Legacy-ERP ohne API | ❌ | ✅ |

**Faustregel:**
> Wenn die Datenquelle eine saubere REST API oder eine relationale Datenbank hat,
> reicht der Generic Adapter für 80% der Kunden.
> Custom Adapter bleiben als bezahlter Implementierungsservice für komplexe Fälle.

---

## Sicherheit

Da Datenbankzugangsdaten und API-Keys gespeichert werden:

- Credentials werden **verschlüsselt** in der DB gespeichert (AES-256)
- JDBC-Verbindungen laufen mit **Read-Only-Rechten** – keine Schreibzugriffe
- REST-Calls gehen über einen **Proxy** – keine direkten Kundenverbindungen
- Alle Zugriffe werden **geloggt** (wer hat wann welche Daten abgerufen)

---

## Erweiterbarkeit

Neue Datenquellen oder Transformationen lassen sich als Plugin registrieren:

```java
public interface AdapterPlugin {
    AdapterType getType();
    List<Map<String, Object>> fetch(AdapterConfig config);
    boolean testConnection(AdapterConfig config);
}

public interface TransformPlugin {
    TransformType getType();
    Object transform(Object value, String param);
}
```

Agenturen oder Kunden können eigene Plugins entwickeln und registrieren –
ohne illusion-Core anzufassen.

> Ziel: Neue Datenquellen ohne Entwicklungsaufwand in illusion einbinden –
> direkt im Wizard, in unter 10 Minuten.

---

## Das Problem

Aktuell benötigt jede neue Datenquelle einen eigenen Adapter:

```
Kundendaten → [eigener Adapter, extra Entwicklung] → illusion
```

Das ist eine Einstiegshürde die den 10-Minuten-Wizard verhindert.

---

## Die Lösung: Generic Adapter

illusion bringt einen konfigurierbaren Generic Adapter mit,
der die häufigsten Datenquellen ohne Code-Entwicklung anbindet:

```
Kundendaten → [Generic Adapter, konfiguriert im Wizard] → illusion
```

---

## Unterstützte Datenquellen

### Stufe 1: Kein Code, sofort nutzbar

**REST API**
- URL + optionaler Auth-Header (Bearer Token, Basic Auth, API Key)
- illusion ruft die API direkt ab
- Unterstützt: JSON-Arrays, paginierte Responses
- Beispiel: `GET https://erp.kunde.com/api/products`

**Relationale Datenbank (JDBC)**
- JDBC-URL + Credentials + SQL-Query
- illusion verbindet sich direkt
- Unterstützt: MySQL, PostgreSQL, MSSQL, Oracle
- Beispiel: `SELECT * FROM products WHERE active = 1`

**CSV / Excel Upload**
- Datei hochladen, Trennzeichen wählen
- illusion erkennt Spalten automatisch
- Unterstützt: UTF-8, verschiedene Trennzeichen, erste Zeile als Header

**FTP / SFTP**
- Host, Pfad, Credentials
- Datei wird regelmäßig abgerufen (Cronjob konfigurierbar)
- Unterstützt: CSV, JSON, XML

### Stufe 2: Custom Adapter (Entwickler, 1-2 Tage)
Für komplexe Fälle die der Generic Adapter nicht abdeckt:
- Legacy-Systeme (SOAP, proprietäre Formate)
- Komplexe verschachtelte Datenstrukturen
- Spezielle Authentifizierungsverfahren
- Echtzeit-Daten via Websocket / Kafka

---

## Architektur

### AdapterConfig – Konfiguration im Wizard gespeichert

```java
@Entity
public class AdapterConfig {
    private String tenantId;
    private AdapterType type;        // REST, JDBC, CSV, FTP, CUSTOM
    private String sourceUrl;        // URL, JDBC-URL oder FTP-Host
    private String authType;         // BEARER, BASIC, API_KEY, NONE
    private String authCredential;   // verschlüsselt gespeichert
    private String query;            // SQL-Query (nur JDBC)
    private String rootPath;         // JSON-Pfad zur Daten-Liste z.B. "result.items"
    private SyncSchedule schedule;   // REALTIME, HOURLY, DAILY, MANUAL
}
```

### GenericAdapterService – Daten abholen

```java
@Service
public class GenericAdapterService {

    public List<Map<String, Object>> fetch(AdapterConfig config) {
        return switch (config.getType()) {
            case REST   -> fetchFromRest(config);
            case JDBC   -> fetchFromDatabase(config);
            case CSV    -> fetchFromCsv(config);
            case FTP    -> fetchFromFtp(config);
        };
    }

    private List<Map<String, Object>> fetchFromRest(AdapterConfig config) {
        return restClient.get()
            .uri(config.getSourceUrl())
            .header("Authorization", resolveAuth(config))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    private List<Map<String, Object>> fetchFromDatabase(AdapterConfig config) {
        JdbcTemplate jdbc = new JdbcTemplate(
            DataSourceBuilder.create()
                .url(config.getSourceUrl())
                .username(config.getUsername())
                .password(config.getPassword())
                .build()
        );
        return jdbc.queryForList(config.getQuery());
    }
}
```

### FieldAnalyzerService – Felder automatisch erkennen

```java
@Service
public class FieldAnalyzerService {

    public List<FieldProfile> analyze(List<Map<String, Object>> samples) {
        // Erste 20 Datensätze analysieren
        // Für jedes Feld:
        // - Typ erkennen (String, Number, Boolean, URL, Date, Nested)
        // - Befüllungsgrad ermitteln (wie viele % haben einen Wert?)
        // - Beispielwert speichern
        // - Priorität vorschlagen (name/title/description → hoch)
    }
}
```

---

## Wizard-Flow im Detail

```
┌─────────────────────────────────────────────────────────┐
│  Schritt 1: Datenquelle wählen                          │
│                                                         │
│  ○ REST API      ○ Datenbank      ○ CSV      ○ FTP      │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Schritt 2: Verbindung konfigurieren (Beispiel REST)    │
│                                                         │
│  URL:   https://erp.kunde.com/api/products              │
│  Auth:  Bearer Token  [••••••••••••••••]                │
│  Pfad:  (optional) result.items                         │
│                                                         │
│  [Verbindung testen]                                    │
│  ✅ Erreichbar – 1.247 Datensätze gefunden              │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Schritt 3: Felder erkannt (automatisch)                │
│                                                         │
│  Feld            Typ       Befüllung  Beispiel          │
│  ─────────────────────────────────────────────          │
│  title           String    100%       "Bohrmaschine"    │
│  voltage         Number    98%        18                │
│  description     String    87%        "Professionelle…" │
│  imageUrl        URL       76%        "https://..."     │
│  weight          Number    91%        1.9               │
│  discontinued    Boolean   100%       false             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Schritt 4: Template wählen & Mapping per Drag & Drop   │
│                                                         │
│  [Produktkatalog Standard] [Datenblatt] [Minimalistisch]│
│                                                         │
│  Template-Platzhalter    DB-Feld                        │
│  ────────────────────    ──────────────────             │
│  [ Haupttitel      ] ←── title          ✅              │
│  [ Beschreibung    ] ←── description    ✅              │
│  [ Bild            ] ←── imageUrl       ✅              │
│  [ Technisch 1     ] ←── voltage        ✅              │
│  [ Technisch 2     ]     weight   (noch nicht gemappt)  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Schritt 5: Vorschau mit echten Daten                   │
│                                                         │
│  ┌───────────────────────────────┐                      │
│  │  Bohrmaschine GSB 18V         │                      │
│  │  Professionelle Bohrmaschine… │                      │
│  │  [Produktbild]                │                      │
│  │  Spannung: 18 V               │                      │
│  └───────────────────────────────┘                      │
│                                                         │
│  [← Zurück]        [Katalog veröffentlichen →]          │
└─────────────────────────────────────────────────────────┘
```

---

## Grenzen des Generic Adapters

| Szenario | Generic Adapter | Custom Adapter |
|---|---|---|
| REST API mit JSON-Array | ✅ | |
| REST API mit verschachteltem JSON | ⚠️ mit rootPath | ✅ |
| Relationale Datenbank | ✅ | |
| SOAP / XML-Webservices | ❌ | ✅ |
| Kafka / Echtzeit-Events | ❌ | ✅ |
| Proprietäre Binärformate | ❌ | ✅ |
| Komplexe Join-Logik | ⚠️ eingeschränkt | ✅ |
| Legacy-ERP ohne API | ❌ | ✅ |

**Faustregel:**
> Wenn die Datenquelle eine saubere REST API oder eine relationale Datenbank hat,
> reicht der Generic Adapter für 80% der Kunden.
> Custom Adapter bleiben als bezahlter Implementierungsservice.

---

## Sicherheit

Da Datenbankzugangsdaten und API-Keys gespeichert werden:

- Credentials werden **verschlüsselt** in der DB gespeichert (AES-256)
- JDBC-Verbindungen laufen in einer **Sandbox** mit Read-Only-Rechten
- REST-Calls gehen über einen **Proxy** – keine direkten Kundenverbindungen
- Alle Zugriffe werden **geloggt** (wer hat wann welche Daten abgerufen)

---

## Erweiterbarkeit

Neue Datenquellen lassen sich als Plugin registrieren:

```java
public interface AdapterPlugin {
    AdapterType getType();
    List<Map<String, Object>> fetch(AdapterConfig config);
    boolean testConnection(AdapterConfig config);
}
```

Agenturen oder Kunden können eigene Plugins entwickeln und registrieren –
ohne illusion-Core anzufassen.
