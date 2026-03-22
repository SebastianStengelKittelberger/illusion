# Illusion – Implementierungsstand

> Übersicht über alle Ideen aus den Konzept-Dokumenten und ihren aktuellen Umsetzungsstand.
> Stand: März 2026

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
| Pflegeoberfläche (UI) | ❌ | POC – nicht Teil des aktuellen Scopes |
| Template-Engine / Webseiten-Generierung | ❌ | POC – nur Datentransformation |

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
| Versionierung der Konfiguration | 💡 | `Environment`-Enum in MapConfig empfohlen |
| Sandbox / Staging-Umgebung | 💡 | `DRAFT → STAGING → PRODUCTION` Pipeline konzipiert |
| 4-Augen-Prinzip für Promotion | 💡 | |
| Low-Code Transformationen im UI | 💡 | |
| API-Gateway Funktion | 💡 | |
| Kafka-Integration | 💡 | Eventgetrieben, für hohe Änderungsvolumen |
| Kubernetes / Cloud-native | 📋 | Architektur passt, `@ConditionalOnProperty` vorhanden |
| Health Endpoints (Actuator) | 📋 | Spring Boot gibt das fast geschenkt |
| Distributed Caching (Redis) | 📋 | Aktuell In-Memory-Cache (`@Cacheable`) |
| Distributed Tracing (Zipkin/Jaeger) | 💡 | |
| Docker Compose für lokalen Start | 💡 | |
| OpenAPI/Swagger | 💡 | |
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
2. **Actuator Health Endpoint** – 1 Zeile in `application.yaml`, sofort Kubernetes-ready
3. **OpenAPI/Swagger** – 1 Dependency, alle Endpoints automatisch dokumentiert
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
| Direkt aus ES laden (nicht alle SKUs) | 📋 | TODO im Code: „Kompletter Overkill an Ressourcen" |
| Dynamische Konfig-Auswahl (nach Land/Sprache/Produkttyp) | 📋 | Aktuell hardcoded `"example"` |
| Java-Code-Ausführung in MapConfig | 📋 | `javaCode`-Feld vorhanden, wird nicht ausgeführt |
| Fehlerbehandlung | 📋 | Kein Exception Handling im Controller |
| Caching (Configs + gerenderte Seiten) | 💡 | |
| Fallback-Logik (`isFallback`-Flag) | 📋 | Flag definiert, nicht verwendet |
| Mehrsprachige Labels | 📋 | Aktuell nur Deutsch hardcoded |
| Kategorienmapping | 💡 | |
| Unit-Tests | 📋 | Nur leerer Test-Stub vorhanden |
