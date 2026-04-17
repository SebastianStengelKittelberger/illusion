# Illusion – Implementierungsstand

> Übersicht über alle Ideen aus den Konzept-Dokumenten und ihren aktuellen Umsetzungsstand.
> Stand: 12. April 2026

---

## Legende

| Symbol | Bedeutung |
|--------|-----------|
| ✅ | Implementiert |
| 🚧 | In Arbeit / teilweise |
| 📋 | Geplant / nächste Schritte |
| 💡 | Langfristige Idee |
| ❌ | Bewusst nicht umgesetzt (POC) |

---

## Kernarchitektur

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Mapping-Engine (MapConfig) | ✅ | `MapConfig`, `MappingHandler`, `IndexingService` |
| Daten aus Adapter laden | ✅ | `ElasticsearchProductLoadService` via ES |
| Elasticsearch-Integration | ✅ | Lesen + Schreiben, Index `illusion-{country}-{language}` |
| Mehrsprachigkeit / Locale | ✅ | Pro `country`/`language` eigener ES-Index |
| Text-Mapping | ✅ | `TextMappingHandler` |
| Bild-Mapping | ✅ | `ImageMappingHandler` |
| Java-Code-Mapping | ✅ | `JavaCodeMappingHandler`, `JavaParserService` |
| Komplexes Mapping (Technische Daten) | ✅ | `ComplexMappingHandler`, sortiert nach ukeyOrder |
| Produktvarianten-Mapping | ✅ | `ProductVariantMappingHandler` |
| **Filter-Feature** | ✅ | `FilterIndexContributor`, `FilterConfig` in MapConfig, Typen STANDARD + PREDICATE |
| Datenqualitäts-Prüfung | ✅ | `DataQualityService`, `DataQualityController` inkl. Werte-Drilldown |
| REST API Endpoints | ✅ | `IndexController`, `InformationController`, `DataQualityController`, `FilterLabelController`, `FilterConfigController` |
| ProductType + ObjAttr Modell | ✅ | inkl. AttrClass, AttrClassRef |
| Pflegeoberfläche (UI) | ✅ | **Summerlight** – React/TS/Vite SPA auf Port 5173 |
| Template-Engine / Webseiten-Generierung | ✅ | **Moonlight** – Thymeleaf-basiert, Slot-System, Port 8078 |
| Unit-Tests | ✅ | 143 Tests, alle grün (inkl. Spring Context-Test) |

---

## Elasticsearch – Indizes

| Index | Beschreibung |
|-------|--------------|
| `illusion-{land}-{sprache}` | Gemappte Produktdaten inkl. `filters`-Objekt |
| `illusion-mapping-config` | MapConfig-Versionen pro Land/Sprache (append-only, Timestamp) |
| `illusion-labels` | Namespace-basierter Labels-Speicher (`namespace: "filter"` für Filter-Labels) |
| `bosch-products`, `bosch-references`, `bosch-media-objects`, `bosch-domain` | Rohdaten vom bosch.adapter |
| `moonlight-vorlagen` | Globale HTML-Slot-Templates |
| `moonlight-vorlagen-history` | Versionshistorie pro Vorlage (bis 20 Einträge) |
| `moonlight-pages` | Seitenkonfigurationen pro Country/Language |
| `moonlight-labels` | Übersetzungs-Labels pro Country/Language |
| `moonlight-routes` | URL-Routing-Tabellen pro Country/Language |

---

## Filter-Feature

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| `FilterType` Enum | ✅ | `STANDARD` (kopiert gemappten Wert) / `PREDICATE` (SpEL mit Ukey-Substitution) |
| `FilterConfig` in `MapConfig` | ✅ | `enabled`, `filterType`, `predicate`, `order`, `group` |
| `FilterIndexContributor` | ✅ | Schreibt `filters: {UKEY: value}` in jedes Produkt-Dokument |
| STANDARD-Filter | ✅ | Kopiert Wert aus `targetField` nach `filters.UKEY` |
| PREDICATE-Filter | ✅ | SpEL-Ausdruck mit Ukey-Token-Substitution (`{{UKEY}}`) |
| Filter-Labels in ES | ✅ | `ElasticsearchLabelService`, Index `illusion-labels`, Namespace `filter` |
| Filter-Labels REST API | ✅ | `GET/PUT /{country}/{language}/filter-labels` |
| Filter-Config REST API | ✅ | `GET /{country}/{language}/filter-config` → `FilterConfigEntry[]` |
| Filter-Labels in Summerlight pflegbar | ✅ | `FilterLabelEditor` unter `/filter-labels` |
| Filter-Config im MapConfig-Editor | ✅ | Toggle + Typ-Auswahl + Predicate-Feld + Order + Group |

---

## Elasticsearch – Technik

| Idee | Status | Anmerkung |
|------|--------|-----------|
| illusion schreibt gemappte Daten in ES | ✅ | `ElasticsearchIndexService`, Bulk-API mit Rollback |
| bosch.adapter schreibt Rohdaten in ES | ✅ | 4 Index-Services |
| illusion liest ausschließlich aus ES | ✅ | Alle Load-Services |
| search_after Pagination (kein OOM) | ✅ | `_doc`-Sort, PAGE_SIZE konfigurierbar |
| Index mit `dynamic: false` | ✅ | Verhindert Überschreitung des 1000-Felder-Limits |
| RestClient als Spring Bean | ✅ | `ElasticsearchConfig.elasticsearchRestClient()` – shared zwischen Services |
| Facetten-Suche | 💡 | Filterbare Suche nach Produkteigenschaften |
| Volltextsuche / Autocomplete | 💡 | Mehrsprachig, Suggest |
| "Ähnliche Produkte" (More-Like-This) | 💡 | |

---

## Daten-Intelligenz

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Datenqualitäts-Dashboard | ✅ | Frontend + Backend; Fortschrittsbalken, Drilldown, UKEY-Suche |
| DQ Drilldown: Fehlende SKUs | ✅ | Tab „Fehlend" – alle SKUs ohne den UKEY |
| DQ Drilldown: Vorhandene Werte | ✅ | Tab „Vorhanden" – SKU + gemappter Wert, filterbar |
| Vollständigkeits-Score pro Produkt | ✅ | `DataQuality` Model mit Score |
| Automatische Feldvorschläge | 💡 | |
| Datenvererbung (Varianten erben) | 💡 | |
| Automatische Übersetzung (DeepL) | 💡 | |
| KI-gestützte Änderungsvorschläge | 💡 | |

---

## Technische Infrastruktur

| Idee | Status | Anmerkung |
|------|--------|-----------|
| MappingConfig in ES | ✅ | Index `illusion-mapping-config`, append-only mit Timestamp |
| MappingConfig REST API | ✅ | GET/PUT `/{country}/{language}/mapping-config` |
| CORS für UI | ✅ | `WebConfig.java` |
| Ukeys in mapped/unmapped aufteilen | ✅ | `InformationService` |
| Ergebnis cachen (Illusion) | ✅ | `@Cacheable("information")` In-Memory |
| Caching Moonlight (Caffeine) | ✅ | 30s TTL für pages, vorlagen, labels, mapping-config, product-data |
| Health Endpoints (Actuator) | 📋 | Spring Boot gibt das fast geschenkt |
| Distributed Caching (Redis) | 📋 | Aktuell In-Memory / Caffeine |
| Authentifizierung / Zugriffsschutz | 📋 | Kein Login, offen für jeden im Netzwerk |
| Draft → Staging → Production Pipeline | 📋 | Konzept in PRODUKTVISION.md ausgearbeitet; `environment`-Feld noch nicht in Modellen |
| Distributed Tracing | 💡 | |
| Docker Compose | 💡 | |
| Webhooks | 💡 | |
| DSGVO-Konformität / Audit-Log | 💡 | |

---

## Generic Adapter Konzept

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| REST API als Datenquelle | ✅ | bosch.adapter als kundenspezifische Implementierung |
| Normalisierung in illusion-internes Format | ✅ | `MapProductDTOService`, Attribute-Modell |
| Wizard-Flow (UI) | 💡 | Konzept ausgearbeitet, nicht implementiert |
| JDBC / Datenbank direkt | 💡 | |
| CSV / Excel Upload | 💡 | |
| Generic Adapter (kein Custom-Code) | 💡 | Aktuell: kundenspezifischer bosch.adapter |

---

## Summerlight – Pflege-UI

> Pfad: `/Users/sebastianstengel/work/summerlight` | Port: 5173 (Vite Dev)
> Start: `cd /Users/sebastianstengel/work/summerlight && npm run dev`
> GitHub: https://github.com/SebastianStengelKittelberger/summerlight

### Screens

| Screen | Route | Status | Beschreibung |
|--------|-------|--------|--------------|
| Ukey-Explorer | `/ukeys` | ✅ | Zwei-Spalten-Grid: Gemappt / Nicht gemappt, SKU + PRODUCT |
| Mapping Config Liste | `/configs` | ✅ | CRUD-Tabelle, Import/Export JSON, „Alle anwenden" |
| Mapping Config Editor | `/editor` | ✅ | Formular mit bedingten Feldern, Monaco für JAVA_CODE, Filter-Sektion |
| Template Editor | `/templates` | ✅ | File-Explorer-Layout, Split-Preview, Versionierung, Undo/Redo, Visual Edit Mode |
| Datenqualitäts-Dashboard | `/quality` | ✅ | Fortschrittsbalken, UKEY-Suche, Drilldown-Tabs, DQ→Mapping-Navigation |
| URL-Routing Editor | `/routing` | ✅ | Routing-Tabelle pflegen: URL → Seite (CMS oder Produkt) |
| **Filter-Labels Editor** | `/filter-labels` | ✅ | Anzeigenamen für Filter-Ukeys pflegen (`illusion-labels`, Namespace `filter`) |

### Template Editor – Features

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| File-Explorer (Seiten + Vorlagen) | ✅ | Linke Sidebar, klappbar, Seiten zeigen Verwendungen |
| Verwendungen ausklappen | ✅ | Klick auf Seite zeigt zugewiesene Slots darunter |
| Drag & Drop Vorlage → Seite | ✅ | Kopiert Vorlage, weist sie der Zielseite zu, speichert in ES |
| Vorlage umbenennen | ✅ | Inline-Rename, updated alle Seiten-Refs |
| Vorlage löschen | ✅ | Bestätigung, DELETE-Endpoint in Moonlight |
| Verwendung entfernen | ✅ | Klick, aktualisiert Seiten-Config |
| UKey-Modals (Gemappt / Nicht gemappt) | ✅ | Chip-Grid mit Suche, Klick fügt an Cursorposition ein |
| Rechtsklick → „UKey mappen…" | ✅ | Monaco-Kontextmenü erkennt `$skuAttr(UKEY)$` unter Cursor |
| Undo/Redo (Seiten-Config) | ✅ | History-Stack, reset beim Seitenwechsel |
| Split-View Preview | ✅ | Toggle, iframe auf Moonlight, Auto-Refresh beim Speichern |
| Versionierung | ✅ | Verlauf-Button, History-Modal, Wiederherstellen |
| **Visual Edit Mode** | ✅ | Fullscreen-Preview, Klick auf Element öffnet UKey-Picker |
| Visual Edit: Bild-Elemente | ✅ | IMG-Tags werden erkannt, Thumbnail in Modal |
| Visual Edit: Leere Felder klickbar | ✅ | `:empty` CSS sichert Mindestgröße für nicht befüllte Felder |
| Quick-Mapping aus Edit Mode | ✅ | Unmapped UKey → Mapping direkt erstellen → Template aktualisiert |

### Allgemeine Features

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Country/Language Selector | ✅ | Global im Header |
| Horizontale Top-Navigation | ✅ | Top-Bar mit allen Bereichen |
| Zustand-Store (Zustand) | ✅ | Country, Language, Configs, Toast |
| Toast-Benachrichtigungen | ✅ | Grün/Rot, auto-dismiss |
| Re-Indexieren | ✅ | Button in Top-Nav, Länder/Sprach-Auswahl |
| TargetFieldType LIST | ✅ | Neben STRING und IMAGE wählbar |
| Authentifizierung | 📋 | Kein Login – geplant: Passwort + Rollen |
| Staging-Prozess | 📋 | Geplant: Draft → Staging → Production für Configs und Templates |

---

## Moonlight – Template Engine

> Pfad: `/Users/sebastianstengel/work/moonlight` | Port: 8078
> Start: IntelliJ oder `./mvnw spring-boot:run`

```
bosch.adapter → [ES illusion-{land}-{sprache}] → moonlight (Rendering) → HTML
```

### ES-Indizes

| Index | Beschreibung |
|-------|--------------|
| `moonlight-vorlagen` | Globale HTML-Slot-Templates |
| `moonlight-vorlagen-history` | Versionshistorie pro Vorlage (bis 20 Einträge) |
| `moonlight-pages` | Seitenkonfigurationen pro Country/Language |
| `moonlight-labels` | Globale Labels pro Country/Language |
| `moonlight-routes` | URL-Routing-Tabellen pro Country/Language |
| `illusion-mapping-config` | MapConfig – Single Source of Truth (gelesen von Moonlight) |

### Implementiert

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Produktseiten-Endpoint | ✅ | `GET /{country}/{language}/product-{sku}?page=&editMode=` |
| Thymeleaf-Integration | ✅ | String-Templates dynamisch aus ES geladen |
| Slot-basiertes Rendering | ✅ | Seite aus Vorlagen (stage, description, benefits, …) |
| Seiten/Vorlagen/Labels-System | ✅ | Vollständig ES-basiert |
| Custom Template-Syntax | ✅ | `$skuAttr(UKEY)$.method()` → Thymeleaf-Expressions |
| Null-safe Feldauflösung | ✅ | Fehlende Felder rendern leer, kein Crash |
| Label-System | ✅ | `§label.key§` → OGNL-kompatible Ausdrücke |
| Graceful Degradation | ✅ | Nicht gemappte Ukeys → `th:text="''"` |
| Caffeine Cache (30s TTL) | ✅ | pages, vorlagen, labels, mapping-config, product-data |
| Versionierung Vorlagen | ✅ | History-Index, Restore via Frontend |
| CRUD Vorlagen | ✅ | GET / PUT / DELETE `/vorlage/{name}` |
| CRUD Seiten | ✅ | GET / PUT `/{country}/{language}/page/{name}` |
| **Visual Edit Mode** | ✅ | `?editMode=true` annotiert `$skuAttr()`-Tokens mit `data-illusion-*` |
| **URL-Routing-System** | ✅ | `RoutingController`, Catch-All, CMS-Seiten, Produktseiten via Route |
| Legacy-Endpoints entfernt | ✅ | Keine Backward-Compatibility-Endpoints (neue Anwendung) |

### Offen

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Dynamische Seiten-Auswahl nach Produkttyp | 📋 | Aktuell: Default `"produktseite"` |
| Fehlerbehandlung im Controller | 📋 | Kein globales Exception Handling |
| Publish/Draft-Status | 📋 | Alle Änderungen sofort live |
| Authentifizierung | 📋 | Kein Token-Schutz auf Endpoints |

---

## Ausgabe-Flexibilität

| Idee | Status | Anmerkung |
|------|--------|-----------|
| REST API für Frontend | ✅ | `InformationController` liefert gemappte Daten |
| HTML-Seitengenerierung via Template-Engine | ✅ | Moonlight – vollständig implementiert |
| Multi-Channel Publishing (PDF, Feed, Marktplatz) | 💡 | |
| Statische Seitengenerierung (CDN) | 💡 | |
| Export-Formate (JSON-Feed, XML, PDF-Katalog) | 💡 | |

---

## Kollaboration & Workflow

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Versionierung Vorlagen | ✅ | Moonlight History-Index |
| Draft → Staging → Production Pipeline | 📋 | Konzept ausgearbeitet, nicht implementiert |
| Rollen & Rechte | 📋 | Geplant für Summerlight |
| Publish/Unpublish-Workflow | 📋 | Alle Änderungen sofort live |
| Änderungshistorie / Audit Log | 💡 | |
| Kommentarfunktion | 💡 | |
| Echtzeit-Collaboration | 💡 | |

---

## Quick Wins (nächste sinnvolle Schritte)

1. **Authentifizierung** – Login für Summerlight (JWT/Session) + Rollen (Admin/Redakteur) + API-Keys für Backend
2. **Draft → Staging → Production** – `environment`-Feld in MapConfig + Template, Promotion-Workflow in Summerlight
3. **Dynamisches Produkttyp-Routing** – Moonlight wählt Seite dynamisch nach Produkttyp
4. **Actuator Health Endpoint** – 1 Zeile in `application.yaml`, sofort Monitoring-ready
5. **Redis Cache** – für horizontales Skalieren statt In-Memory / Caffeine
6. **Docker Compose** – lokaler Start aller Services mit einem Befehl
7. **Filter ausbauen** – weitere Ukeys mit `filterConfig.enabled = true` + Labels in Summerlight pflegen
