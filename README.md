# Getting Started

## Übersicht

**illusion** ist ein datengetriebenes Content-Management-System für Produktkataloge.
Es bildet beliebige Datenquellen (Produktdatenbanken, APIs, ERP) über eine konfigurierbare
Mapping-Engine auf Webseiten-Templates ab – ohne Programmierkenntnisse für die tägliche Pflege.

## Systemarchitektur

```
bosch.adapter  →  [ES: Rohdaten]  →  illusion  →  [ES: gemappte Daten]  →  moonlight  →  HTML
                                         ↕
                                    summerlight (UI)
```

| Dienst | Port | Beschreibung |
|--------|------|--------------|
| **illusion** | 8079 | Mapping-Engine, REST API, Indexierung |
| **moonlight** | 8078 | Thymeleaf Template-Engine, HTML-Rendering, URL-Routing |
| **summerlight** | 5173 | React/TS Pflege-UI |
| **Elasticsearch** | 9200 | Persistenz für alle Dienste |

## Start

```bash
# Elasticsearch
cd ~/work/elasticsearch-9.3.1 && bin/elasticsearch

# illusion
cd ~/work/illusion && ./mvnw spring-boot:run

# moonlight (IntelliJ oder)
cd ~/work/moonlight && ./mvnw spring-boot:run

# summerlight
cd ~/work/summerlight && npm run dev
```

## ES-Indizes

| Index | Inhalt |
|-------|--------|
| `illusion-{land}-{sprache}` | Gemappte Produktdaten |
| `illusion-mapping-config` | MappingConfig-Versionen pro Land/Sprache |
| `bosch-products`, `bosch-references` etc. | Rohdaten vom Adapter |
| `moonlight-vorlagen` | Globale HTML-Slot-Templates |
| `moonlight-vorlagen-history` | Versionshistorie pro Vorlage |
| `moonlight-pages` | Seitenkonfigurationen (Slots) pro Country/Language |
| `moonlight-labels` | Übersetzungs-Labels pro Country/Language |
| `moonlight-routes` | URL-Routing-Tabellen pro Country/Language |

## Summerlight – Bereiche

| Route | Beschreibung |
|-------|--------------|
| `/ukeys` | UKeys erkunden, gemappt vs. ungemappt |
| `/configs` | Mapping-Regeln verwalten (CRUD, Import/Export) |
| `/editor` | Mapping-Config im Detail bearbeiten |
| `/templates` | HTML-Vorlagen + Seiten + Visual Edit Mode |
| `/routing` | URL-Routing-Tabelle pflegen |
| `/quality` | Datenqualitäts-Dashboard |

## Moonlight – Wichtige Endpoints

| Endpoint | Beschreibung |
|----------|--------------|
| `GET /{country}/{language}/product-{sku}?page=&editMode=` | Produktseite rendern |
| `GET /{country}/{language}/**` | Catch-All: URL aus Routing-Tabelle rendern |
| `GET/PUT /{country}/{language}/routes` | URL-Routing-Tabelle lesen/speichern |
| `GET/PUT /{country}/{language}/page/{name}` | Seitenkonfiguration |
| `GET/PUT /{country}/{language}/labels` | Labels |
| `GET/PUT /vorlage/{name}` | HTML-Vorlage lesen/speichern |

## Stand

Vollständigen Implementierungsstand: siehe `IMPLEMENTIERUNGSSTAND.md`

Noch offen / langfristig:
- Authentifizierung / Zugriffsschutz
- Draft → Staging → Production Pipeline
- Generic Adapter Wizard (kein Custom-Code nötig)
- Dynamisches Produkttyp-Routing
- Variantenmanagement
- SEO-Felder, Sitemap
- Redis Cache für horizontales Skalieren


