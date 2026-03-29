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
| **moonlight** | 8078 | Thymeleaf Template-Engine, HTML-Rendering |
| **summerlight** | 5175 | React/TS Pflege-UI |
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
| `moonlight-pages` | Seitenkonfigurationen (Slots) |
| `moonlight-labels` | Übersetzungs-Labels pro Land/Sprache |

## Stand

Dies ist ein POC. Entsprechend ist hier vieles vereinfacht oder nur angedeutet.

Noch offen / langfristig:
- Datenqualitäts-Dashboard Frontend
- Filter nach AttrClass/Kategorie/ProductType
- Draft → Staging → Production Pipeline
- Generic Adapter Wizard (kein Custom-Code nötig)
- Variantenmanagement
- SEO-Felder, Sitemap

Vollständigen Implementierungsstand: siehe `IMPLEMENTIERUNGSSTAND.md`

