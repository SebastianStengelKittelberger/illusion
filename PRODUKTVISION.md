# Produktvision & Differenzierung

## Was ist illusion?

Ein datengetriebenes Content-Management-System für Produktkataloge, das beliebige Datenquellen
(Produktdatenbanken, APIs, ERP-Systeme) über eine konfigurierbare Mapping-Engine auf
Webseiten-Templates abbildet – ohne Programmierkenntnisse für die tägliche Pflege.

---

## Geplante Kernfunktionen

| Modul | Beschreibung |
|---|---|
| **illusion-core** | Mapping-Engine (MapConfig, Templates, Rendering) |
| **illusion-ui** | Pflege-Oberfläche für MapConfig und Templates |
| **illusion-preview** | Vorschau und Publishing-Workflow |
| **illusion-web** | Webkatalog-Ausgabe |
| **illusion-analytics** | Datenqualität und Monitoring |

---

## Differenzierung gegenüber Pimcore, Akeneo & Co.

### 1. Datenquellen-Agilität
Standard-CMS kennen eine primäre Datenquelle. illusion kombiniert beliebig viele:
```
Produkt-DB + PIM + ERP + externe API + Excel
        ↓
   ein einheitliches Ausgabeformat
```
Kein Standard-CMS kann das ohne massive Eigenentwicklung.

### 2. Mapping-Transparenz
Kunden sehen jederzeit, **woher** ein Wert auf der Webseite kommt:
> „Dieses Feld kommt aus Tabelle X, Spalte Y, transformiert durch Regel Z"

Für Kunden mit Compliance-Anforderungen (DSGVO, Produkthaftung) ist das ein kritisches Feature.

### 3. Datenqualitäts-Engine
- Vollständigkeitsprüfung pro Produkt
- Pflichtfelder definierbar
- Warnungen bei fehlenden Übersetzungen
- Score pro Produkt („80% vollständig")

Vergleichbar mit Akeneo – aber ohne den Akeneo-Preis.

### 4. Enterprise Java-Stack
Für Enterprise-Kunden ist PHP (Pimcore) oft ein Hindernis:
- Nativ integrierbar in bestehende Microservice-Landschaften
- SSO mit Active Directory / LDAP
- CI/CD nach Unternehmensstandard mit Maven
- Spring Security, Spring Boot – bekannte Technologien im Enterprise-Umfeld

### 5. Live-Mapping statt Import-Batch
Pimcore importiert Daten – die können veralten. illusion lädt Daten live aus der Quelle,
cached sie intelligent und regeneriert bei Änderungen automatisch.

### 6. No-Code Mapping für Nicht-Entwickler
Das MapConfig-Konzept als intuitive UI:
> Redakteur zieht Datenbankfeld per Drag & Drop auf Template-Platzhalter

Zugeschnitten auf den spezifischen Use Case: Produktdaten → Webseite.

---

## Vergleichsmatrix

| Feature | Pimcore | Akeneo | illusion |
|---|---|---|---|
| Mehrere Datenquellen | ⚠️ Workaround | ❌ | ✅ Native |
| Mapping-Transparenz | ❌ | ❌ | ✅ |
| Enterprise Java | ❌ PHP | ❌ PHP | ✅ |
| Datenqualitäts-Engine | ⚠️ Basic | ✅ | ✅ |
| Live-Daten | ❌ Batch | ❌ Batch | ✅ möglich |
| Komplexe Mapping-Logik | ⚠️ PHP-Code | ❌ | ✅ Java |
| No-Code Pflege | ✅ | ✅ | ✅ geplant |
| Open Source | ✅ CE | ❌ | ✅ |

---

## Erweiterungen mit hoher Kundenrelevanz

### Sofort relevant
- **Mehrsprachigkeit / Lokalisierung** – Felder und Templates pro Sprache
- **Preview-Funktion** – Vorschau vor dem Publizieren
- **Publish/Unpublish-Workflow** – ohne Deployment
- **Änderungshistorie** – Audit Log (wer hat wann was geändert)
- **Rollen & Rechte** – Redakteur vs. Admin

### Für größere Kunden
- **Scheduler** – zeitgesteuertes Publizieren
- **Variantenmanagement** – gemeinsame Basis, unterschiedliche Felder
- **SEO-Felder** – Meta-Title, Description, Canonical URL
- **Sitemap-Generierung** – automatisch aus Produktseiten

### Differenzierungsmerkmale
- **Mapping-Vorschau** – Echtzeit-Vorschau des DB-Werts beim Konfigurieren
- **Datenqualitäts-Dashboard** – fehlende Felder, Vollständigkeits-Score
- **A/B-Testing** – zwei Template-Varianten vergleichen
- **Webhook-Support** – externe Systeme bei Änderungen benachrichtigen
- **Export-Formate** – PDF-Katalog, JSON-Feed, XML für Marktplätze

---

## Der Pitch

> *„illusion ist das einzige System, das komplexe Produktdaten aus mehreren Quellen
> transparent, live und ohne PHP auf eine Webseite bringt –
> konfigurierbar ohne Entwickler."*

---

## Hybrid-Architektur: illusion als Kern

illusion muss nicht alles alleine können. Die stärkste Variante ist eine Kombination
aus automatisiertem Produktkatalog, optionaler Business-Logik und einem klassischen CMS
für redaktionelle Inhalte.

### Gesamtarchitektur

```
Produktdatenbank (z.B. Bosch XML)
        ↓
bosch.adapter          ← Rohdaten aufbereiten, REST API bereitstellen
        ↓
illusion               ← Mapping konfigurierbar ohne Entwickler
        ↓
[Transformation Layer] ← optionale Business-Logik durch Entwickler
        ↓
Frontend API           ← einheitliche Schnittstelle für das Frontend
        ↑
herkömmliches CMS      ← redaktionelle Seiten (Blog, Landingpages, Navigation)
```

### Was der Transformation Layer ermöglicht
Ein optionaler Service zwischen illusion und Frontend – einmalig vom Entwickler gebaut,
danach stabil:
- **Preislogik** – Rabatte, Staffelpreise berechnen
- **Verfügbarkeit** – Lagerbestand aus ERP einmischen
- **Personalisierung** – andere Daten je nach Nutzergruppe oder Markt
- **Business Rules** – „Zeige dieses Produkt nur in DE"

### Aufgabenteilung mit einem CMS

| Inhaltstyp | Quelle |
|---|---|
| Produktdaten | illusion (automatisch aus DB) |
| Produktbeschreibungen | illusion + manuelle Ergänzung |
| Kategorieseiten | CMS (Redakteur) |
| Landingpages | CMS (Redakteur) |
| Blog / News | CMS (Redakteur) |
| Navigation | CMS (Redakteur) |

Das Frontend kombiniert beide Quellen auf einer Seite:
```
GET /illusion/products/123    → Produktdaten (automatisch)
GET /cms/pages/bohrmaschinen  → Redaktioneller Inhalt (manuell gepflegt)
```

### Warum das strategisch stark ist

**Best of both worlds** – kein System muss alles können.
illusion liefert Produktdaten, das CMS liefert Redaktion.

**Schrittweiser Einstieg für Kunden:**
- Phase 1: Nur Produktkatalog mit illusion
- Phase 2: Redaktionelle Inhalte mit CMS ergänzen
- Phase 3: Transformation Layer für individuelle Business-Logik

**Kein Vendor-Lock-in** – Frontend und CMS sind austauschbar.
illusion ist der stabile, produktdatengetriebene Kern.

**Markterprobtes Muster** – Shopify + Contentful + eigene API ist in modernem
E-Commerce Standard. illusion bietet das als vollständige, integrierte Lösung.

---

## Der erweiterte Pitch

> *„illusion ist der produktdatengetriebene Kern eures Webauftritts –
> kombinierbar mit jedem CMS für redaktionelle Inhalte
> und erweiterbar durch eigene Business-Logik,
> ohne das Grundsystem anzufassen."*

---

## Skalierbarkeit & Cloud-Native

### Microservice-Architektur als Wettbewerbsvorteil

Während Pimcore und Akeneo monolithische PHP-Anwendungen sind, ist illusion
von Grund auf als Microservice-Landschaft gebaut:

```
bosch.adapter  →  illusion-core  →  illusion-ui
     ↕                 ↕                ↕
[x3 Pods]         [x10 Pods]        [x2 Pods]
```

Jeder Service skaliert **unabhängig** – genau dort wo der Engpass ist.

### Kubernetes-native Skalierungsszenarien

**Hohe Lesezugriffe auf den Katalog:**
```yaml
illusion-web: replicas: 20
illusion-core: replicas: 2   # bleibt klein
bosch.adapter: replicas: 1
```

**Großer Reindex (z.B. 500.000 Produkte):**
```yaml
illusion-core: replicas: 10  # temporär hochskalieren
# danach wieder runter → Kosten sparen
```

### Was Kubernetes konkret bringt

| Feature | Nutzen für illusion |
|---|---|
| **Auto-Scaling (HPA)** | Bei Last automatisch mehr Pods |
| **Rolling Updates** | Kein Downtime bei Deployments |
| **Health Checks** | Ausgefallene Pods automatisch neustarten |
| **Resource Limits** | Kosten kontrollierbar |
| **Namespaces** | Multi-Tenant sauber trennen |
| **ConfigMaps/Secrets** | Kundenkonfigurationen sicher verwalten |

### SaaS-Fähigkeit durch Kubernetes

Pimcore und Akeneo sind primär für On-Premise gedacht – SaaS ist dort nachträglich
eingebaut. Bei illusion ist Cloud-native der primäre Betriebsmodus:

```
Kunde A  →  eigener Namespace  →  eigene Adapter-Config
Kunde B  →  eigener Namespace  →  eigene Adapter-Config
Kunde C  →  eigener Namespace  →  eigene Adapter-Config
                    ↓
         shared illusion-core (spart Kosten)
         shared Elasticsearch
         shared Monitoring
```

### Kubernetes-Readiness – was noch fehlt

Spring Boot gibt vieles davon fast geschenkt:

| Was | Aufwand |
|---|---|
| Health Endpoints (`/actuator/health`) | ⭐ minimal |
| Externalisierte Konfiguration | ⭐ minimal |
| Distributed Caching (Redis statt In-Memory) | ⭐⭐ mittel |
| Distributed Tracing (Zipkin/Jaeger) | ⭐⭐⭐ größer |

### Fazit Skalierbarkeit

> illusion ist architekturell für Cloud-native Betrieb gebaut –
> Pimcore und Akeneo sind es nicht.
> Bei Enterprise-Kunden mit eigener Kubernetes-Infrastruktur
> ist das ein sofort sichtbarer, echter technischer Vorsprung.

---

## Environment-Pipeline: Draft → Staging → Production

### Konzept

Keine Änderung geht direkt live. Jede Konfiguration durchläuft eine Pipeline:

```
DRAFT ──→ STAGING ──→ PRODUCTION
  ↑           ↑            ↑
Redakteur   Tester      Freigabe
 editiert   prüft      explizit
```

Promotion ist nur vorwärts möglich – nie überspringen.
Production ist erst erreichbar wenn Staging erfolgreich war.

### Was der Pipeline unterliegt

| Was | Warum wichtig |
|---|---|
| **MapConfigs** | Feldmapping-Fehler erst auf Staging sichtbar |
| **Templates** | Layout-Fehler mit echten Daten prüfen |
| **Java-Mapping-Code** | Logikfehler auf Testdaten fangen |
| **Datenquelle-Verbindungen** | Neue Adapter erst gegen Testdaten laufen lassen |

### Versionierung automatisch

Jede Promotion erstellt einen unveränderlichen Snapshot:
```
MapConfig "TITLE → name"
├── v1  PRODUCTION  ← aktuell live
├── v2  STAGING     ← in Prüfung
└── v3  DRAFT       ← in Arbeit
```
Rollback = v1 wieder auf Production aktivieren – ein Klick.

### Optionales 4-Augen-Prinzip

Staging → Production braucht Freigabe einer zweiten Person.
Für Enterprise-Kunden und regulierte Branchen (Medizintechnik, Chemie) oft Pflicht.

### Warum das ein starkes Differenzierungsmerkmal ist

Bei Pimcore geht ein Klick direkt live. illusion macht das professioneller:

| | Ohne Pipeline | Mit Pipeline |
|---|---|---|
| Fehler in Production | passiert schnell | durch Staging gefangen |
| Rollback | manuell, aufwändig | ein Klick |
| Nachvollziehbarkeit | unklar | vollständige Historie |
| Enterprise-Tauglichkeit | ❌ | ✅ |
| Regulierte Branchen | ❌ | ✅ |

### Architekturhinweis

`Environment` sollte von Anfang an als Feld in `MapConfig` und `Template` eingeplant
werden – auch wenn die Pipeline-UI erst später gebaut wird:

```java
enum Environment { DRAFT, STAGING, PRODUCTION }

// In MapConfig und Template:
private Environment environment = Environment.DRAFT;
```
