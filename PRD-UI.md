# PRD – Illusion & Moonlight Configuration UI

## Überblick

Eine React-basierte Konfigurationsoberfläche für das **Illusion**-System (Datenmapping) und **Moonlight** (Template-Rendering). Die UI richtet sich an technische Redakteure und Integratoren, die ohne Programmierkenntnisse Mapping-Konfigurationen erstellen und HTML-Templates pflegen.

---

## Ziele

- Visuelle Erstellung und Verwaltung von `MapConfig`-Einträgen
- Auswahl von Ukeys per Klick statt manueller Eingabe
- Template-Verwaltung für Moonlight
- Keine Programmierkenntnisse für Standardfälle erforderlich
- Single Page Application (SPA), kommuniziert **direkt** mit Illusion- und Moonlight-REST-APIs (kein eigenes Backend)

---

## Nutzergruppen

| Nutzer | Aufgabe |
|---|---|
| Integration Engineer | Mapping-Konfigurationen anlegen und testen |
| Content Redakteur | Templates für Moonlight erstellen/pflegen |
| QA / Datenpflege | Datenqualität einsehen und Mapping nachvollziehen |

---

## Screens / Features

---

### 1. Screen: Ukey Explorer

**Zweck:** Übersicht aller verfügbaren Attribute-Schlüssel (Ukeys) des Systems als Ausgangspunkt für die Konfiguration.

**Verhalten:**
- Nutzer wählt zunächst `Country` und `Language` (Dropdowns) sowie eine `SKU` (Eingabefeld mit Autosuggest)
- Die UI ruft `POST /{country}/{language}/info/` auf (mit leerer `mapConfigs`-Liste) und zeigt die zurückgegebenen `skuUkeys` und `productUkeys` an
- Die SKU muss **nicht** mitgegeben werden – der InformationController liefert auch ohne SKU die verfügbaren Ukeys
- Ukeys werden gruppiert dargestellt: **SKU-Ebene** / **Produkt-Ebene**
- Jeder Ukey-Eintrag zeigt: Ukey-Name, Datenqualitätswert (falls vorhanden), Typ-Hinweis
- Klick auf einen Ukey → öffnet den **MapConfig-Editor** (s. Screen 2) mit vorausgefülltem `ukey`-Feld
- Suchfeld zum Filtern der Ukey-Liste

**Erforderliche Backend-Erweiterung (InformationController):**
- Neuer Endpoint: `GET /{country}/{language}/info/ukeys?sku={sku}` der alle Ukeys ohne MapConfig-Input zurückgibt
- Optional: `GET /{country}/{language}/info/ukeys` ohne SKU-Filter (alle bekannten Ukeys aus dem Index)
- Alternativ: Das vorhandene `POST /{country}/{language}/info/` mit leerem Body verwenden, falls dies bereits Ukeys zurückliefert

---

### 2. Screen: MapConfig Editor

**Zweck:** Erstellen und Bearbeiten einer einzelnen `MapConfig`.

**Formularfelder:**

| Feld | UI-Element | Werte / Hinweis |
|---|---|---|
| `ukey` | Text (vorausgefüllt via Ukey Explorer) | z. B. `COLOR`, `$NAME$` |
| `dtoType` | Dropdown | `PRODUCT`, `SKU`, `CATEGORY` |
| `mappingType` | Dropdown | `TEXT`, `IMAGE`, `COMPLEX`, `JAVA_CODE`, `PRODUCT_VARIANTS` |
| `targetField` | Textfeld | Name des Zielfeldes im Output-Objekt |
| `targetFieldType` | Dropdown | `STRING`, `IMAGE`, *erweiterbar* |
| `isFallback` | Toggle/Checkbox | Fallback-Mapping aktivieren |
| `target` | Dropdown | `PRODUCT`, `CATEGORY` |
| `javaCode` | Code-Editor (nur wenn `mappingType = JAVA_CODE`) | SpEL-Ausdruck, Syntax-Highlighting |
| `complexMapping` | Sub-Formular (nur wenn `mappingType = COMPLEX`) | Konfiguration untergeordneter Mappings |

**Verhalten:**
- Felder die nicht relevant sind werden ausgeblendet (abhängig von `mappingType`)
- Validierung vor dem Speichern (Pflichtfelder, gültige Enum-Werte)
- „Testen"-Button: Führt das Mapping mit der aktuellen Config gegen eine Test-SKU aus und zeigt das Ergebnis

---

### 3. Screen: MappingConfig Liste / Verwaltung

**Zweck:** Überblick über alle konfigurierten MapConfigs eines Country/Language-Kontexts.

**Features:**
- Tabellarische Darstellung aller `MapConfig`-Einträge
- Sortierung nach `targetField`, `ukey`, `mappingType`
- Bearbeiten / Duplizieren / Löschen einzelner Einträge
- Import/Export als JSON
- „Alle anwenden"-Button: Sendet alle Configs an `POST /{country}/{language}/index`

**Persistenz:**
- MappingConfigs werden in **Elasticsearch** gespeichert
- Die Backend-Implementierung dafür steht noch aus (geplant für späteren Zeitpunkt)
- Bis zur Implementierung: Verwaltung im Browser-LocalStorage oder als herunterladbare JSON-Datei
- Mit der Elasticsearch-Persistenz wird auch eine **Versionierung** der Configs eingeführt

---

### 4. Screen: Template Editor (Moonlight)

**Zweck:** HTML-Templates für Moonlight erstellen und pflegen.

**Mehrsprachigkeit / Länder:**
- Template-Verwaltung erfolgt **pro Land** (Country-Kontext)
- Templates haben einen **übergreifenden Fallback**: Ist für ein Land kein spezifisches Template vorhanden, wird automatisch das generische Fallback-Template verwendet
- Die UI macht die Fallback-Hierarchie sichtbar (z. B. `DE` → Fallback: `default`)

**Features:**
- Liste aller vorhandenen Templates (nach Land und Slot gruppiert: `stage`, `description`, `benefits`, etc.)
- Editor mit Syntax-Highlighting für Thymeleaf/HTML
- Sidebar zeigt verfügbare Ukeys als Snippets zum Einfügen: `$skuAttr(UKEY)$`
- Label-Verwaltung: Verwaltung der `§label.key§`-Platzhalter
- Vorschau-Button: Ruft Moonlight-Endpoint direkt per API auf und zeigt gerenderte HTML-Vorschau
- Speichern via Moonlight-API (Moonlight ist per REST erreichbar)

**Template-Variablen-Syntax (Moonlight):**
```
$skuAttr(UKEY)$.method()    → SKU-Attribut
§label.key§                  → i18n-Label
```

---

### 5. Screen: Datenqualität (optional / Phase 2)

**Zweck:** Übersicht über Vollständigkeit und Qualität der Attributdaten.

**Features:**
- Tabelle: Ukey → Vollständigkeitswert (aus `GET /{country}/{language}/dataQuality/{ukey}/`)
- Farbliche Markierung (grün/gelb/rot)
- Filter nach Schwellwert
- Direktlink in den MapConfig-Editor

---

## Technischer Stack (Empfehlung)

| Bereich | Technologie |
|---|---|
| Framework | React 18+ mit TypeScript |
| Build | Vite |
| UI-Komponenten | shadcn/ui oder MUI |
| State Management | Zustand oder React Query |
| Code-Editor | Monaco Editor (für JAVA_CODE / Templates) |
| HTTP-Client | Axios oder TanStack Query |
| Routing | React Router v6 |

---

## Erforderliche Backend-Erweiterungen (InformationController)

### Ukey-Discovery ohne MapConfig

Das bestehende `POST /{country}/{language}/info/` mit leerem `mapConfigs`-Array und **ohne SKU** liefert bereits `skuUkeys` und `productUkeys` ✅ – kein neuer Endpoint erforderlich.

**Response (bereits vorhanden):**
```json
{
  "skuUkeys": ["COLOR", "DESCRIPTION", "WEIGHT", ...],
  "productUkeys": ["$NAME$", "BRAND", "CATEGORY_ID", ...]
}
```

### Optional: Bekannte Ukeys aus dem Index (ohne SKU)

```
GET /{country}/{language}/info/ukeys/all
```

Liefert alle je gesehenen Ukeys aus dem Elasticsearch-Index (nicht nur für eine SKU). Erfordert eine Aggregations-Query in Elasticsearch.

---

## Nicht in Scope (Phase 1)

- Benutzer-Authentifizierung / Rollenverwaltung
- Versionierung von MapConfigs (kommt mit der Elasticsearch-Persistenz)
- Direktes Schreiben von Templates via Moonlight-API (Moonlight-seitige Implementierung steht noch aus)
- Bulk-SKU-Vorschau

---

## Voraussetzungen (vor UI-Implementierung)

Folgende Punkte müssen geklärt oder implementiert sein, bevor die UI-Entwicklung beginnt:

| # | Thema | Beschreibung |
|---|---|---|
| 1 | **BFF – Backend for Frontend** | Die UI bekommt ein eigenes Backend. Hintergrund: Illusion wird zukünftig regelmäßige Mapping-Jobs abarbeiten müssen; ein dediziertes BFF trennt diese Last von der UI-Kommunikation und ermöglicht bessere Performance und Entkopplung. Wird gesondert implementiert. |
| 2 | **MapConfig-Persistenz in Elasticsearch** | Speichern und Laden von MapConfigs via Elasticsearch (inkl. Versionierung). Wird gesondert implementiert. |
| 3 | **Moonlight Template-API** | CRUD-Endpoints in Moonlight zum Lesen und Schreiben von Templates. |

---



| Frage | Entscheidung |
|---|---|
| Eigenes Backend (BFF)? | ✅ Ja – aus Performance-Gründen (s. Voraussetzungen) |
| MapConfig-Persistenz? | Elasticsearch (Backend-Impl. steht noch aus) |
| Template-Verwaltung länderübergreifend? | Pro Land, mit übergreifendem Fallback |
| Moonlight per API erreichbar? | ✅ Ja |

## Offene Fragen

1. Welche Moonlight-API-Endpoints sollen für Template-CRUD implementiert werden?
2. Wie sieht die Fallback-Hierarchie für Templates konkret aus? (z. B. `DE` → `EU` → `default`?)
