# Illusion – Implementierungsstand

> Übersicht über alle Ideen aus den Konzept-Dokumenten und ihren aktuellen Umsetzungsstand.
> Stand: 29. März 2026 (aktualisiert)

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
| Daten aus Adapter laden | ✅ | `LoadDataService` via Elasticsearch |
| Elasticsearch-Integration | ✅ | Lesen + Schreiben, Index `illusion-{country}-{language}` |
| Mehrsprachigkeit / Locale | ✅ | Pro `country`/`language` eigener ES-Index |
| Text-Mapping | ✅ | `TextMappingHandler` |
| Bild-Mapping | ✅ | `ImageMappingHandler` |
| Java-Code-Mapping | ✅ | `JavaCodeMappingHandler`, `JavaParserService` |
| Komplexes Mapping (Technische Daten) | ✅ | `ComplexMappingHandler`, sortiert nach ukeyOrder |
| Produktvarianten-Mapping | ✅ | `ProductVariantMappingHandler` |
| Filter (z.B. nach AttrClass, Kategorie) | 📋 | Noch nicht implementiert |
| Datenqualitäts-Prüfung | ✅ | `DataQualityService`, `DataQualityController` inkl. Werte-Drilldown |
| DQ Werte-Drilldown | ✅ | `GET /{country}/{language}/dataQuality/{ukey}/values` → SKU+Wert-Paare |
| REST API Endpoints | ✅ | `IndexController`, `InformationController`, `DataQualityController` |
| ProductType + ObjAttr Modell | ✅ | inkl. AttrClass, AttrClassRef |
| Pflegeoberfläche (UI) | ✅ | **Summerlight** – React/TS/Vite SPA auf Port 5173 |
| Template-Engine / Webseiten-Generierung | ✅ | **Moonlight** – Thymeleaf-basiert, Seiten/Vorlagen-System, Port 8078 |

---

## Elasticsearch

| Idee | Status | Anmerkung |
|------|--------|-----------|
| illusion schreibt gemappte Daten in ES | ✅ | `ElasticsearchIndexService`, Bulk-API mit Rollback |
| bosch.adapter schreibt Rohdaten in ES | ✅ | 4 Index-Services |
| illusion liest ausschließlich aus ES | ✅ | Alle Load-Services |
| search_after Pagination (kein OOM) | ✅ | `_doc`-Sort, PAGE_SIZE konfigurierbar |
| Index mit `dynamic: false` | ✅ | Verhindert Überschreitung des 1000-Felder-Limits |
| Facetten-Suche | 💡 | |
| Filter (AttrClass, Kategorie, ProductType) | 📋 | |
| Volltextsuche / Autocomplete | 💡 | |

---

## Daten-Intelligenz

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Datenqualitäts-Dashboard | ✅ | Frontend + Backend; Fortschrittsbalken, Drilldown, UKEY-Suche |
| DQ Drilldown: Fehlende SKUs | ✅ | Tab „❌ Fehlend" – alle SKUs ohne den UKEY |
| DQ Drilldown: Vorhandene Werte | ✅ | Tab „✅ Vorhanden" – SKU + gemappter Wert, filterbar |
| Vollständigkeits-Score pro Produkt | ✅ | `DataQuality` Model mit Score |
| Automatische Feldvorschläge | 💡 | |
| Datenvererbung (Varianten erben) | 💡 | |
| Automatische Übersetzung (DeepL) | 💡 | |
| KI-gestützte Änderungsvorschläge | 💡 | |

---

## Technische Infrastruktur

| Idee | Status | Anmerkung |
|------|--------|-----------|
| MappingConfig in ES | ✅ | Index `illusion-mapping-config`, versioniert per Timestamp |
| MappingConfig REST API | ✅ | GET/PUT `/{country}/{language}/mapping-config` |
| CORS für UI | ✅ | `WebConfig.java` |
| Ukeys in mapped/unmapped aufteilen | ✅ | `InformationService` |
| Ergebnis cachen (Illusion) | ✅ | `@Cacheable("information")` In-Memory |
| Caching Moonlight (Caffeine) | ✅ | 30s TTL für pages, vorlagen, labels, mapping-config, product-data |
| Health Endpoints (Actuator) | 📋 | Spring Boot gibt das fast geschenkt |
| Distributed Caching (Redis) | 📋 | Aktuell In-Memory / Caffeine |
| Authentifizierung / Zugriffsschutz | 📋 | Kein Login, offen für jeden im Netzwerk |
| Distributed Tracing | 💡 | |
| Docker Compose | 💡 | |

---

## Generic Adapter Konzept

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| REST API als Datenquelle | ✅ | bosch.adapter als kundenspezifische Implementierung |
| Normalisierung in illusion-internes Format | ✅ | `MapProductDTOService` |
| Wizard-Flow (UI) | 💡 | |
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
| Mapping Config Editor | `/editor` | ✅ | Formular mit bedingten Feldern, Monaco für JAVA_CODE, URL-Param `?ukey=X` |
| Template Editor | `/templates` | ✅ | File-Explorer-Layout, Split-Preview, Versionierung, Undo/Redo, Visual Edit Mode |
| Datenqualitäts-Dashboard | `/quality` | ✅ | Fortschrittsbalken, UKEY-Suche, Drilldown-Tabs, DQ→Mapping-Navigation |
| URL-Routing Editor | `/routing` | ✅ | Routing-Tabelle pflegen: URL → Seite (CMS oder Produkt) |

### Template Editor – Features

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| File-Explorer (Seiten + Vorlagen) | ✅ | Linke Sidebar, klappbar, Seiten zeigen Verwendungen |
| Verwendungen ausklappen | ✅ | Klick auf Seite zeigt zugewiesene Slots darunter |
| Drag & Drop Vorlage → Seite | ✅ | Kopiert Vorlage, weist sie der Zielseite zu, speichert in ES |
| Vorlage umbenennen | ✅ | ✏ Hover-Button, Inline-Rename, updated alle Seiten-Refs |
| Vorlage löschen | ✅ | 🗑 Hover-Button, Bestätigung, DELETE-Endpoint in Moonlight |
| Verwendung entfernen | ✅ | ✕ im aufgeklappten Slot, aktualisiert Seiten-Config |
| UKey-Modals (Gemappt / Nicht gemappt) | ✅ | Chip-Grid mit Suche, Klick fügt an Cursorposition ein |
| Rechtsklick → „UKey mappen…" | ✅ | Monaco-Kontextmenü erkennt `$skuAttr(UKEY)$` unter Cursor |
| Undo/Redo (Seiten-Config) | ✅ | ↩/↪ Buttons, History-Stack, reset beim Seitenwechsel |
| Split-View Preview | ✅ | ⊞ Toggle, iframe auf Moonlight, Auto-Refresh beim Speichern |
| Versionierung | ✅ | 🕐 Verlauf-Button, History-Modal, ↩ Wiederherstellen |
| **Visual Edit Mode** | ✅ | Fullscreen-Preview, Klick auf Element öffnet UKey-Picker, direkt im Template speichern |
| Visual Edit: Bild-Elemente | ✅ | IMG-Tags werden erkannt, Thumbnail in Modal angezeigt |
| Visual Edit: Leere Felder klickbar | ✅ | `:empty` CSS sichert Mindestgröße für nicht befüllte Felder |
| Quick-Mapping aus Edit Mode | ✅ | Unmapped UKey → Mapping direkt erstellen → Template wird aktualisiert |

### Allgemeine Features

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Country/Language Selector | ✅ | Global im Header |
| Horizontale Top-Navigation | ✅ | Top-Bar mit 6 Bereichen |
| Zustand-Store (Zustand) | ✅ | Country, Language, Configs, Toast |
| Toast-Benachrichtigungen | ✅ | Grün/Rot, auto-dismiss |
| Re-Indexieren | ✅ | Button in Top-Nav, Länder/Sprach-Auswahl |
| TargetFieldType LIST | ✅ | Neben STRING und IMAGE als Mapping-Zieltyp wählbar |
| Authentifizierung | 📋 | Kein Login, offen für jeden im Netzwerk |

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
| Liest direkt aus ES | ✅ | `DataService` liest aus `illusion-{country}-{language}` |
| Eine MappingConfig pro Land | ✅ | Moonlight liest immer Illusion's MappingConfig |
| Custom Template-Syntax | ✅ | `$skuAttr(UKEY)$.method()` → Thymeleaf-Expressions |
| Null-safe Feldauflösung | ✅ | Fehlende Felder rendern leer, kein Crash |
| Label-System | ✅ | `§label.key§` → OGNL-kompatible Ausdrücke |
| Graceful Degradation | ✅ | Nicht gemappte Ukeys → `th:text="''"` |
| Caffeine Cache (30s TTL) | ✅ | pages, vorlagen, labels, mapping-config, product-data |
| Versionierung Vorlagen | ✅ | History-Index, GET /history, Restore via Frontend |
| CRUD Vorlagen | ✅ | GET / PUT / DELETE `/vorlage/{name}` |
| CRUD Seiten | ✅ | GET / PUT `/{country}/{language}/page/{name}` |
| **Visual Edit Mode** | ✅ | `?editMode=true` annotiert alle `$skuAttr()`-Tokens mit `data-illusion-*` Attributen + Highlight-CSS |
| Edit Mode: Standalone-Tokens | ✅ | Erzeugt `<span data-illusion-ukey="..." class="illusion-editable">` |
| Edit Mode: Attribut-Tokens (th:text, th:src) | ✅ | Injiziert `data-illusion-*` in das öffnende HTML-Tag |
| Edit Mode: Leere Felder klickbar | ✅ | `:empty` CSS mit `min-width/min-height` + UKey-Label als Pseudo-Content |
| **URL-Routing-System** | ✅ | `RoutingController`, `RoutingStorageService`, `RouteConfig` |
| Routing: Catch-All-Controller | ✅ | `GET /{country}/{language}/**` – nachschlagen in Routing-Tabelle |
| Routing: CMS-Seiten | ✅ | `renderCmsPage()` – Rendering ohne SKU, nur Labels + Slots |
| Routing: Produktseiten via Route | ✅ | `{sku}`-Platzhalter im URL-Pattern → SKU wird extrahiert |
| Routing: CRUD | ✅ | GET/PUT `/{country}/{language}/routes` |

### Offen

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Dynamische Seiten-Auswahl nach Produkttyp | 📋 | Aktuell: Default `"produktseite"` |
| Fehlerbehandlung im Controller | 📋 | Kein globales Exception Handling |
| Publish/Draft-Status | 📋 | Alle Änderungen sofort live |
| Authentifizierung | 📋 | Kein Token-Schutz auf Endpoints |

---

## Quick Wins (nächste sinnvolle Schritte)

1. **Authentifizierung** – einfaches Login für Summerlight + API-Keys für Moonlight
2. **Draft → Staging → Production Pipeline** – Änderungen nicht sofort live
3. **Actuator Health Endpoint** – 1 Zeile in `application.yaml`
4. **Dynamisches Produkttyp-Routing** – Moonlight wählt Seite nach Produkttyp
5. **Redis Cache** – für horizontales Skalieren
6. **Filter** – nach AttrClass, Kategorie, ProductType in Illusion

## Elasticsearch

| Idee | Status | Anmerkung |
|------|--------|-----------|
| illusion schreibt gemappte Daten in ES | ✅ | `ElasticsearchIndexService`, Bulk-API mit Rollback |
| bosch.adapter schreibt Rohdaten in ES | ✅ | 4 Index-Services |
| illusion liest ausschließlich aus ES | ✅ | Alle Load-Services |
| search_after Pagination (kein OOM) | ✅ | `_doc`-Sort, PAGE_SIZE konfigurierbar |
| Index mit `dynamic: false` | ✅ | Verhindert Überschreitung des 1000-Felder-Limits |
| Facetten-Suche | 💡 | |
| Filter (AttrClass, Kategorie, ProductType) | 📋 | |
| Volltextsuche / Autocomplete | 💡 | |

---

## Daten-Intelligenz

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Datenqualitäts-Dashboard | ✅ | Frontend + Backend; Fortschrittsbalken, Drilldown, UKEY-Suche |
| DQ Drilldown: Fehlende SKUs | ✅ | Tab „❌ Fehlend" – alle SKUs ohne den UKEY |
| DQ Drilldown: Vorhandene Werte | ✅ | Tab „✅ Vorhanden" – SKU + gemappter Wert, filterbar |
| Vollständigkeits-Score pro Produkt | ✅ | `DataQuality` Model mit Score |
| Automatische Feldvorschläge | 💡 | |
| Datenvererbung (Varianten erben) | 💡 | |
| Automatische Übersetzung (DeepL) | 💡 | |
| KI-gestützte Änderungsvorschläge | 💡 | |

---

## Technische Infrastruktur

| Idee | Status | Anmerkung |
|------|--------|-----------|
| MappingConfig in ES | ✅ | Index `illusion-mapping-config`, versioniert per Timestamp |
| MappingConfig REST API | ✅ | GET/PUT `/{country}/{language}/mapping-config` |
| CORS für UI | ✅ | `WebConfig.java` |
| Ukeys in mapped/unmapped aufteilen | ✅ | `InformationService` |
| Ergebnis cachen (Illusion) | ✅ | `@Cacheable("information")` In-Memory |
| Caching Moonlight (Caffeine) | ✅ | 30s TTL für pages, vorlagen, labels, mapping-config, product-data |
| Health Endpoints (Actuator) | 📋 | Spring Boot gibt das fast geschenkt |
| Distributed Caching (Redis) | 📋 | Aktuell In-Memory / Caffeine |
| Authentifizierung / Zugriffsschutz | 📋 | Kein Login, offen für jeden im Netzwerk |
| Distributed Tracing | 💡 | |
| Docker Compose | 💡 | |

---

## Generic Adapter Konzept

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| REST API als Datenquelle | ✅ | bosch.adapter als kundenspezifische Implementierung |
| Normalisierung in illusion-internes Format | ✅ | `MapProductDTOService` |
| Wizard-Flow (UI) | 💡 | |
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
| Mapping Config Editor | `/editor` | ✅ | Formular mit bedingten Feldern, Monaco für JAVA_CODE, URL-Param `?ukey=X` |
| Template Editor | `/templates` | ✅ | File-Explorer-Layout, Split-Preview, Versionierung, Undo/Redo |
| Datenqualitäts-Dashboard | `/quality` | ✅ | Fortschrittsbalken, UKEY-Suche, Drilldown-Tabs, DQ→Mapping-Navigation |

### Template Editor – Features

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| File-Explorer (Seiten + Vorlagen) | ✅ | Linke Sidebar, klappbar, Seiten zeigen Verwendungen |
| Verwendungen ausklappen | ✅ | Klick auf Seite zeigt zugewiesene Slots darunter |
| Drag & Drop Vorlage → Seite | ✅ | Kopiert Vorlage, weist sie der Zielseite zu, speichert in ES |
| Vorlage umbenennen | ✅ | ✏ Hover-Button, Inline-Rename, updated alle Seiten-Refs |
| Vorlage löschen | ✅ | 🗑 Hover-Button, Bestätigung, DELETE-Endpoint in Moonlight |
| Verwendung entfernen | ✅ | ✕ im aufgeklappten Slot, aktualisiert Seiten-Config |
| UKey-Modals (Gemappt / Nicht gemappt) | ✅ | Chip-Grid mit Suche, Klick fügt an Cursorposition ein |
| Rechtsklick → „UKey mappen…" | ✅ | Monaco-Kontextmenü erkennt `$skuAttr(UKEY)$` unter Cursor |
| Undo/Redo (Seiten-Config) | ✅ | ↩/↪ Buttons, History-Stack, reset beim Seitenwechsel |
| Split-View Preview | ✅ | ⊞ Toggle, iframe auf Moonlight, Auto-Refresh beim Speichern |
| Versionierung | ✅ | 🕐 Verlauf-Button, History-Modal, ↩ Wiederherstellen |
| Verwendungen aus Vorlagenliste filtern | ✅ | Kopien (`vorlage-seite`) erscheinen nur unter Seite, nicht global |

### Allgemeine Features

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Country/Language Selector | ✅ | Global im Header |
| Horizontale Top-Navigation | ✅ | Umgebaut von linker Sidebar zu Top-Bar |
| Zustand-Store (Zustand) | ✅ | Country, Language, Configs, Toast |
| Toast-Benachrichtigungen | ✅ | Grün/Rot, auto-dismiss |
| Re-Indexieren | ✅ | Button in Top-Nav, Länder/Sprach-Auswahl |
| Authentifizierung | 📋 | Kein Login, offen für jeden im Netzwerk |

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
| `illusion-mapping-config` | MapConfig – Single Source of Truth (gelesen von Moonlight) |

### Implementiert

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Spring Boot REST-Endpoint | ✅ | `GET /{country}/{language}/product-{sku}?page=produktseite` |
| Thymeleaf-Integration | ✅ | String-Templates dynamisch aus ES geladen |
| Slot-basiertes Rendering | ✅ | Seite aus Vorlagen (stage, description, benefits, …) |
| Seiten/Vorlagen/Labels-System | ✅ | Vollständig ES-basiert |
| Liest direkt aus ES | ✅ | `DataService` liest aus `illusion-{country}-{language}` |
| Eine MappingConfig pro Land | ✅ | Moonlight liest immer Illusion's MappingConfig |
| Custom Template-Syntax | ✅ | `$skuAttr(UKEY)$.method()` → Thymeleaf-Expressions |
| Null-safe Feldauflösung | ✅ | Fehlende Felder rendern leer, kein Crash |
| Label-System | ✅ | `§label.key§` → OGNL-kompatible Ausdrücke |
| Graceful Degradation | ✅ | Nicht gemappte Ukeys → `th:text="''"` |
| Caffeine Cache (30s TTL) | ✅ | pages, vorlagen, labels, mapping-config, product-data |
| Versionierung Vorlagen | ✅ | History-Index, GET /history, Restore via Frontend |
| CRUD Vorlagen | ✅ | GET / PUT / DELETE `/vorlage/{name}` |
| CRUD Seiten | ✅ | GET / PUT `/{country}/{language}/page/{name}` |

### Offen

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Dynamische Seiten-Auswahl nach Produkttyp | 📋 | Aktuell: Default `"produktseite"` |
| Fehlerbehandlung im Controller | 📋 | Kein globales Exception Handling |
| Publish/Draft-Status | 📋 | Alle Änderungen sofort live |
| Unit-Tests | 📋 | |

---

## Quick Wins (nächste sinnvolle Schritte)

1. **Produkttyp-Routing** – Moonlight wählt Seite dynamisch nach Produkttyp statt hardcoded "produktseite"
2. **Authentifizierung** – einfaches Login für Summerlight + API-Keys für Moonlight
3. **Actuator Health Endpoint** – 1 Zeile in `application.yaml`
4. **Publish/Draft-Status** – Vorlagen als Entwurf markieren
5. **Filter** – nach AttrClass, Kategorie, ProductType in Illusion
6. **Redis Cache** – für horizontales Skalieren

---

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
| Daten aus Adapter laden | ✅ | `LoadDataService` via Elasticsearch |
| Elasticsearch-Integration | ✅ | Lesen + Schreiben, Index `illusion-{country}-{language}` |
| Mehrsprachigkeit / Locale | ✅ | Pro `country`/`language` eigener ES-Index |
| Text-Mapping | ✅ | `TextMappingHandler` |
| Bild-Mapping | ✅ | `ImageMappingHandler` |
| Java-Code-Mapping | ✅ | `JavaCodeMappingHandler`, `JavaParserService` |
| Komplexes Mapping (Technische Daten) | ✅ | `ComplexMappingHandler`, sortiert nach ukeyOrder |
| Produktvarianten-Mapping | ✅ | `ProductVariantMappingHandler` |
| Filter (z.B. nach AttrClass, Kategorie) | 📋 | Noch nicht implementiert |
| Datenqualitäts-Prüfung | ✅ | `DataQualityService`, `DataQualityController` |
| REST API Endpoints | ✅ | `IndexController`, `InformationController`, `DataQualityController` |
| ProductType + ObjAttr Modell | ✅ | inkl. AttrClass, AttrClassRef |
| Pflegeoberfläche (UI) | ✅ | **Summerlight** – React/TS/Vite SPA auf Port 5175 |
| Template-Engine / Webseiten-Generierung | ✅ | **Moonlight** – Thymeleaf-basiert, Seiten/Vorlagen-System, Port 8078 |

---

## Elasticsearch

| Idee | Status | Anmerkung |
|------|--------|-----------|
| illusion schreibt gemappte Daten in ES | ✅ | `ElasticsearchIndexService`, Bulk-API mit Rollback |
| bosch.adapter schreibt Rohdaten in ES | ✅ | 4 Index-Services |
| illusion liest ausschließlich aus ES | ✅ | Alle Load-Services |
| search_after Pagination (kein OOM) | ✅ | `_doc`-Sort, PAGE_SIZE konfigurierbar |
| Index mit `dynamic: false` | ✅ | Verhindert Überschreitung des 1000-Felder-Limits |
| Facetten-Suche | 💡 | |
| Filter (AttrClass, Kategorie, ProductType) | 📋 | |
| Volltextsuche / Autocomplete | 💡 | |

---

## Daten-Intelligenz

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Datenqualitäts-Dashboard | 🚧 | Backend vorhanden, kein Frontend-Screen |
| Vollständigkeits-Score pro Produkt | ✅ | `DataQuality` Model mit Score |
| Automatische Feldvorschläge | 💡 | |
| Datenvererbung (Varianten erben) | 💡 | |
| Automatische Übersetzung (DeepL) | 💡 | |
| KI-gestützte Änderungsvorschläge | 💡 | |

---

## Technische Infrastruktur

| Idee | Status | Anmerkung |
|------|--------|-----------|
| MappingConfig in ES | ✅ | Index `illusion-mapping-config`, versioniert per Timestamp |
| MappingConfig REST API | ✅ | GET/PUT `/{country}/{language}/mapping-config` |
| CORS für UI | ✅ | `WebConfig.java` |
| Ukeys in mapped/unmapped aufteilen | ✅ | `InformationService` |
| Ergebnis cachen | ✅ | `@Cacheable("information")` In-Memory |
| Health Endpoints (Actuator) | 📋 | Spring Boot gibt das fast geschenkt |
| Distributed Caching (Redis) | 📋 | Aktuell In-Memory-Cache |
| Distributed Tracing | 💡 | |
| Docker Compose | 💡 | |

---

## Generic Adapter Konzept

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| REST API als Datenquelle | ✅ | bosch.adapter als kundenspezifische Implementierung |
| Normalisierung in illusion-internes Format | ✅ | `MapProductDTOService` |
| Wizard-Flow (UI) | 💡 | |
| JDBC / Datenbank direkt | 💡 | |
| CSV / Excel Upload | 💡 | |
| Generic Adapter (kein Custom-Code) | 💡 | Aktuell: kundenspezifischer bosch.adapter |

---

## Summerlight – Pflege-UI

> Pfad: `/Users/sebastianstengel/work/summerlight` | Port: 5175 (Vite Dev)
> Start: `cd /Users/sebastianstengel/work/summerlight && npm run dev`
> GitHub: https://github.com/SebastianStengelKittelberger/summerlight

### Screens

| Screen | Route | Status | Beschreibung |
|--------|-------|--------|--------------|
| Ukey-Explorer | `/ukeys` | ✅ | Zwei-Spalten-Grid: Gemappt (links) / Nicht gemappt (rechts), SKU + PRODUCT |
| Mapping Config Liste | `/configs` | ✅ | CRUD-Tabelle, Import/Export JSON, „Alle anwenden" |
| Mapping Config Editor | `/editor` | ✅ | Formular mit bedingten Feldern, Monaco für JAVA_CODE |
| Template Editor | `/templates` | ✅ | Seiten/Vorlagen-System, Monaco HTML-Editor, Ukey-Sidebar, QuickMapModal |

### Features

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Country/Language Selector | ✅ | Global im Header |
| Zustand-Store (Zustand) | ✅ | Country, Language, Configs, Toast |
| Toast-Benachrichtigungen | ✅ | Grün/Rot, auto-dismiss |
| Drag & Drop Ukeys → Monaco | ✅ | |
| Clipboard-Copy | ✅ | |
| QuickMapModal | ✅ | Popup beim Klick auf nicht gemappten Ukey → speichert in Illusion-MappingConfig |
| Seiten-Verwaltung | ✅ | Dropdown, neue Seite anlegen, zwischen Seiten wechseln |
| Vorlagen-Verwaltung | ✅ | Globale HTML-Vorlagen, Dropdown, speichern in `moonlight-vorlagen` |
| Als Verwendung kopieren | ✅ | Vorlage in seitenspezifische Kopie umwandeln (`vorlage-seite`) |
| Labels-Editor | ✅ | Globale Labels pro Country/Language in `moonlight-labels` |
| Re-Indexieren Modal | ✅ | Button in Sidebar, Länder/Sprach-Auswahl, Spinner, Fortschritt |
| Datenqualitäts-Screen | 📋 | Backend vorhanden (`/dataQuality/{ukey}/`), kein Frontend |

---

## Moonlight – Template Engine

> Pfad: `/Users/sebastianstengel/work/moonlight` | Port: 8078
> Start: IntelliJ oder `./mvnw spring-boot:run`

```
bosch.adapter → [ES illusion-{land}-{sprache}] → moonlight (Rendering) → HTML
```

### Architektur

| Komponente | Beschreibung |
|-----------|--------------|
| `moonlight-vorlagen` | Globale HTML-Slot-Templates in ES |
| `moonlight-pages` | Seitenkonfigurationen pro Country/Language in ES |
| `moonlight-labels` | Globale Labels pro Country/Language in ES |

### Implementiert

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Spring Boot REST-Endpoint | ✅ | `GET /{country}/{language}/product-{sku}?page=produktseite` |
| Thymeleaf-Integration | ✅ | String-Templates dynamisch aus ES geladen |
| Slot-basiertes Rendering | ✅ | Seite aus Vorlagen (stage, description, benefits, …) |
| Seiten/Vorlagen/Labels-System | ✅ | Vollständig ES-basiert, keine Classpath-Abhängigkeit |
| Liest direkt aus ES | ✅ | `DataService` liest aus `illusion-{country}-{language}` |
| Eine MappingConfig pro Land | ✅ | Moonlight liest immer Illusion's MappingConfig aus ES |
| Custom Template-Syntax | ✅ | `$skuAttr(UKEY)$.method()` → Thymeleaf-Expressions |
| Null-safe Feldauflösung | ✅ | Fehlende Felder rendern leer, kein Crash |
| Label-System | ✅ | `§label.key§` → `${labels['key'] != null ? ...}` |
| Graceful Degradation | ✅ | Nicht gemappte Ukeys → `th:text="''"`, kein Template-Fehler |
| `@JsonIgnoreProperties` | ✅ | Alte ES-Dokumente mit unbekannten Feldern werden toleriert |

### Offen

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Dynamische Seiten-Auswahl nach Produkttyp | 📋 | Aktuell: Default `"produktseite"` |
| Fehlerbehandlung im Controller | 📋 | Kein Exception Handling |
| Caching (gerenderte Seiten) | 💡 | |
| Unit-Tests | 📋 | |
| Java-Code-Ausführung in MapConfig | 📋 | `javaCode`-Feld vorhanden, wird nicht ausgeführt |

---

## Quick Wins (nächste sinnvolle Schritte)

1. **Datenqualitäts-Dashboard Frontend** – Backend bereits vorhanden
2. **Actuator Health Endpoint** – 1 Zeile in `application.yaml`
3. **Filter** – nach AttrClass, Kategorie, ProductType
4. **Moonlight: Seiten-Auswahl nach Produkttyp** – statt hardcoded "produktseite"
5. **Redis Cache** – für horizontales Skalieren
6. **`Environment`-Feld** – Vorbereitung für Draft/Staging/Production Pipeline


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

## Kernarchitektur (`README.md` / `PRODUKTVISION.md`)

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Mapping-Engine (MapConfig) | ✅ | `MapConfig`, `MappingHandler`, `IndexingService` |
| Daten aus Adapter laden | ✅ | `LoadDataService` via Elasticsearch |
| Elasticsearch-Integration | ✅ | Lesen + Schreiben, Index `illusion-{country}-{language}` |
| Mehrsprachigkeit / Locale | ✅ | Pro `country`/`language` eigener ES-Index |
| Text-Mapping | ✅ | `TextMappingHandler` |
| Bild-Mapping | ✅ | `ImageMappingHandler` |
| Java-Code-Mapping | ✅ | `JavaCodeMappingHandler`, `JavaParserService` |
| Komplexes Mapping (SKU-Attribute / Technische Daten) | ✅ | `ComplexMappingHandler`, sortiert nach ukeyOrder aus ProductType |
| Produktvarianten-Mapping | ✅ | `ProductVariantMappingHandler` |
| Filter (z.B. nach AttrClass, Kategorie) | 📋 | Noch nicht implementiert |
| Datenqualitäts-Prüfung | ✅ | `DataQualityService`, `DataQualityController` |
| REST API Endpoints | ✅ | `IndexController`, `InformationController`, `DataQualityController` |
| ProductType + ObjAttr Modell | ✅ | inkl. AttrClass, AttrClassRef |
| Pflegeoberfläche (UI) | ✅ | **Summernight** – React/TS/Vite SPA auf Port 5175, kommuniziert mit Illusion (8079) & Moonlight (8078) |
| Template-Engine / Webseiten-Generierung | 🚧 | **Moonlight** – Thymeleaf-basiert, Slot-System, läuft auf Port 8078 |

---

## Elasticsearch (`FEATURE_IDEEN.md` → Suche & Elasticsearch)

| Idee | Status | Anmerkung |
|------|--------|-----------|
| illusion schreibt gemappte Daten in ES | ✅ | `ElasticsearchIndexService`, Bulk-API mit Rollback |
| bosch.adapter schreibt Rohdaten in ES | ✅ | 4 Index-Services: Products, References, MediaObjects, Domain |
| illusion liest ausschließlich aus ES | ✅ | `ElasticsearchProductLoadService`, `ElasticsearchReferenceLoadService`, `ElasticsearchMediaObjectLoadService`, `ElasticsearchConfigLoadService` |
| search_after Pagination (kein OOM) | ✅ | `_doc`-Sort, PAGE_SIZE konfigurierbar |
| Index mit `dynamic: false` | ✅ | Verhindert Überschreitung des 1000-Felder-Limits |
| Facetten-Suche | 💡 | Filterbare Suche nach Produkteigenschaften |
| Filter (AttrClass, Kategorie, ProductType etc.) | 📋 | Noch nicht implementiert — logisch eng mit Facetten verwandt |
| Volltextsuche / Autocomplete | 💡 | Mehrsprachig, Suggest |
| „Ähnliche Produkte" (More-Like-This) | 💡 | |
| illusion als Suchplattform (Solr etc.) | 💡 | |

---

## Daten-Intelligenz (`FEATURE_IDEEN.md`)

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Datenqualitäts-Dashboard | 🚧 | `DataQualityService` vorhanden, kein Frontend |
| Vollständigkeits-Score pro Produkt | ✅ | `DataQuality` Model mit Score |
| Automatische Feldvorschläge | 💡 | |
| Datenvererbung (Varianten erben vom Hauptprodukt) | 💡 | |
| Automatische Übersetzung (DeepL) | 💡 | |
| KI-gestützte Änderungsvorschläge | 💡 | |

---

## Ausgabe-Flexibilität (`FEATURE_IDEEN.md`)

| Idee | Status | Anmerkung |
|------|--------|-----------|
| REST API für Frontend | ✅ | `InformationController` liefert gemappte Daten |
| HTML-Seitengenerierung via Template-Engine | 🚧 | **Moonlight** – Thymeleaf-basiert, Slot-System, läuft auf Port 8078 |
| Multi-Channel Publishing (PDF, Feed, Marktplatz) | 💡 | |
| Statische Seitengenerierung (CDN) | 💡 | |
| Export-Formate (JSON-Feed, XML, PDF-Katalog) | 💡 | |

---

## Kollaboration & Workflow (`FEATURE_IDEEN.md` / `PRODUKTVISION.md`)

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Kommentarfunktion | 💡 | |
| Freigabe-Workflow | 💡 | |
| Änderungsvergleich (Git-Diff für Produktdaten) | 💡 | |
| Publish/Unpublish-Workflow | 💡 | |
| Änderungshistorie / Audit Log | 💡 | |
| Rollen & Rechte | 💡 | |

---

## Analyse & Optimierung (`FEATURE_IDEEN.md`)

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Conversion-Tracking | 💡 | |
| SEO-Analyse (Meta-Title, H1, Alt-Texte) | 💡 | |
| SEO-Felder (Meta-Title, Canonical URL) | 💡 | |
| Sitemap-Generierung | 💡 | |
| Wettbewerbsvergleich | 💡 | |

---

## Technische Infrastruktur (`FEATURE_IDEEN.md` / `PRODUKTVISION.md`)

| Idee | Status | Anmerkung |
|------|--------|-----------|
| MappingConfig in Elasticsearch speichern | ✅ | `ElasticsearchMappingConfigService`, Index `illusion-mapping-config`, versioniert per Timestamp |
| MappingConfig REST API | ✅ | `MappingConfigController` GET/PUT `/{country}/{language}/mapping-config` |
| CORS für UI | ✅ | `WebConfig.java` – erlaubt `http://localhost:*` |
| Ukeys in mapped/unmapped aufteilen | ✅ | `InformationService` lädt MappingConfig und splittet in `mappedSkuUkeys`, `unmappedSkuUkeys`, `mappedProductUkeys`, `unmappedProductUkeys` |
| InformationController als GET | ✅ | Kein Request-Body mehr nötig |
| Ergebnis cachen | ✅ | `@Cacheable("information")` – In-Memory per Country/Language |

---

## Summernight – Pflege-UI (separates Projekt)

> Pfad: `/Users/sebastianstengel/work/summernight` | Port: 5175 (Vite Dev)  
> Start: `cd /Users/sebastianstengel/work/summernight && npm run dev`

### Screens

| Screen | Route | Status | Beschreibung |
|--------|-------|--------|--------------|
| Ukey-Explorer | `/ukeys` | ✅ | Zwei-Spalten-Grid (Nicht gemappt / Gemappt), SKU + PRODUCT getrennt, scrollbar |
| Mapping Config Liste | `/configs` | ✅ | CRUD-Tabelle, Import/Export JSON, „Alle anwenden" |
| Mapping Config Editor | `/editor` | ✅ | Formular mit bedingten Feldern, Monaco für JAVA_CODE, Toast-Feedback |
| Template Editor | `/templates` | ✅ | Monaco HTML-Editor, Ukey-Sidebar (mapped/unmapped), Drag & Drop, QuickMapModal, neuer Slot |

### Features

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Country/Language Selector | ✅ | Global im Header, steuert alle Requests |
| Zustand-Store (Zustand) | ✅ | Country, Language, Configs, Toast |
| Toast-Benachrichtigungen | ✅ | Grün/Rot, auto-dismiss nach 3,5s |
| Drag & Drop Ukeys → Monaco | ✅ | Capture-Phase-Listener verhindert Monaco-Konflikt (kein „/" und „0") |
| Clipboard-Copy (SKU/PRD) | ✅ | Hover-Buttons pro Ukey, grünes ✓ als Feedback |
| QuickMapModal | ✅ | Popup beim Klick/Drag auf nicht gemappten Ukey – speichert direkt in MappingConfig |
| Neues Template anlegen | ✅ | `+`-Button neben Slot-Tabs, Inline-Input |
| Scrollbare Ukey-Sidebar | ✅ | Bounded an Editor-Höhe (`overflow-hidden`, `h-full`) |

### Offene Punkte

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Datenqualitäts-Screen | 📋 | Backend vorhanden (`/dataQuality/{ukey}/`), kein Frontend-Screen |
| Cache invalidieren nach MappingConfig-Änderung | 📋 | Aktuell: Cache läuft bis Illusion-Neustart |
| Moonlight-Rendering nutzt gespeicherte Templates | 📋 | `TemplateStorageService` schreibt in ES, Render-Service liest noch Classpath |


| Sandbox / Staging-Umgebung | 💡 | `DRAFT → STAGING → PRODUCTION` Pipeline konzipiert |
| 4-Augen-Prinzip für Promotion | 💡 | |
| Low-Code Transformationen im UI | 💡 | |
| API-Gateway Funktion | 💡 | |
| Health Endpoints (Actuator) | 📋 | Spring Boot gibt das fast geschenkt |
| Distributed Caching (Redis) | 📋 | Aktuell In-Memory-Cache (`@Cacheable`) |
| Distributed Tracing (Zipkin/Jaeger) | 💡 | |
| Docker Compose für lokalen Start | 💡 | |
| Webhooks | 💡 | |
| DSGVO-Konformität / Audit-Log | 💡 | |

---

## Generic Adapter Konzept (`GENERIC_ADAPTER_KONZEPT.md`)

> Ziel: Neue Datenquellen per Wizard ohne Entwicklungsaufwand einbinden.

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| REST API als Datenquelle | ✅ | bosch.adapter als kundenspezifische Implementierung |
| Normalisierung in illusion-internes Format | ✅ | `MapProductDTOService`, Attribute-Modell |
| Wizard-Flow (UI) | 💡 | Konzept ausgearbeitet, nicht implementiert |
| JDBC / Datenbank direkt | 💡 | |
| CSV / Excel Upload | 💡 | |
| FTP / SFTP Datenquelle | 💡 | |
| Generic Adapter (kein Custom-Code) | 💡 | Aktuell: kundenspezifischer bosch.adapter |

---

## Was als nächstes sinnvoll wäre (📋 Quick Wins)

1. **Filter** – nach AttrClass, Kategorie, ProductType; Grundlage für Facetten-Suche
2. **Moonlight: direkt aus ES laden** – aktuell lädt jeder Seitenaufruf alle Produkte
3. **Actuator Health Endpoint** – 1 Zeile in `application.yaml`, sofort Monitoring-ready
4. **`Environment`-Feld in MapConfig** – Vorbereitung für Draft/Staging/Production Pipeline
5. **Redis statt In-Memory Cache** – ermöglicht horizontales Skalieren
6. **Datenqualitäts-Dashboard Frontend** – Backend ist bereits vorhanden
7. **Kategorienmapping** – im README als offen markiert

---

## Offen laut README (ursprüngliche TODO-Liste)

| Offener Punkt | Status |
|---------------|--------|
| Pflegeoberfläche | ❌ POC |
| Modulieren der Daten unabhängig von UKEYs (z.B. DeliveryScope) | 💡 |
| Technische Daten (Königsklasse) | ✅ | `ComplexMappingHandler` mit ukeyOrder-Sortierung via ProductType |
| Kategorienmapping | 💡 |
| Mehrere UKEYs in eine Liste mappen | 💡 |
| Templateanwendung / Stage | 🚧 | **Moonlight** – separates Projekt, POC-Stadium (siehe unten) |
| Daten nicht per Request sondern via DB/Kafka | ✅ | Elasticsearch als Zwischenschicht |

---

## Moonlight – Template Engine (separates Projekt)

> Pfad: `/Users/sebastianstengel/work/moonlight` | Port: 8078

**Konzept:** Moonlight nimmt die gemappten Daten aus Illusion und rendert daraus fertige HTML-Produktseiten via Thymeleaf.

```
bosch.adapter → [ES] → illusion (Mapping) → [ES] → moonlight (Rendering) → HTML
```

### Implementiert (🚧 POC)

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Spring Boot REST-Endpoint | ✅ | `GET /{country}/{language}/product-{sku}` |
| Thymeleaf-Integration | ✅ | 2 Resolver: Classpath-Fragmente + dynamische String-Templates |
| Slot-basiertes Rendering | ✅ | Seite aus Komponenten (stage, description, benefits, …) |
| JSON-Konfiguration laden | ✅ | `ConfigService` lädt `configs/{name}.json` |
| Illusion-Integration | ✅ | `DataService` ruft Illusion REST API auf |
| Custom Template-Syntax | ✅ | `$skuAttr(UKEY)$.method()` → Thymeleaf-Expressions |
| Label-System | ✅ | `§label.key§` in Templates |
| Beispiel-Templates (Bosch Produkt) | ✅ | Stage, Description, Benefits |

### Noch offen / Baustellen

| Feature | Status | Anmerkung |
|---------|--------|-----------|
| Moonlight: direkt aus ES laden (nicht alle SKUs) | 📋 | TODO im Code: „Kompletter Overkill an Ressourcen" |
| Moonlight: Templates in ES gespeichert | ✅ | `TemplateStorageService` schreibt/liest aus `moonlight-slot-templates` + `moonlight-template-config` |
| Moonlight: Render-Service nutzt ES-Templates | 📋 | Aktuell noch Classpath-Fallback aktiv |
| Dynamische Konfig-Auswahl (nach Land/Sprache/Produkttyp) | 📋 | Aktuell hardcoded `"example"` |
| Java-Code-Ausführung in MapConfig | 📋 | `javaCode`-Feld vorhanden, wird nicht ausgeführt |
| Fehlerbehandlung | 📋 | Kein Exception Handling im Controller |
| Caching (Configs + gerenderte Seiten) | 💡 | |
| Fallback-Logik (`isFallback`-Flag) | 📋 | Flag definiert, nicht verwendet |
| Mehrsprachige Labels | 📋 | Aktuell nur Deutsch hardcoded |
| Kategorienmapping | 💡 | |
| Unit-Tests | 📋 | Nur leerer Test-Stub vorhanden |
