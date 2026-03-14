# Illusion – Feature-Ideen & Erweiterungen

> Diese Ideen sind langfristige Erweiterungsmöglichkeiten.
> Sie sind nach strategischem Wert gruppiert, nicht nach Priorität.
> Jede Idee zieht erheblichen Entwicklungsaufwand nach sich.

---

## Daten-Intelligenz

### Automatische Feldvorschläge
illusion analysiert die Datenquelle und schlägt automatisch vor, welche Felder
für welche Template-Platzhalter sinnvoll sind.
- Spart Konfigurationsaufwand erheblich
- Besonders wertvoll beim Onboarding neuer Kunden

### Datenvererbung
Produktvarianten erben Felder vom Hauptprodukt – nur Unterschiede werden überschrieben:
```
Bohrmaschine (Basis)
├── 18V Variante  ← erbt alles, überschreibt nur Spannung
└── 36V Variante  ← erbt alles, überschreibt nur Spannung
```

### Automatische Übersetzung
Integration mit DeepL/Google Translate:
- Fehlende Übersetzungen werden automatisch befüllt
- Zur manuellen Prüfung markiert
- Spart Übersetzungsaufwand bei vielen Sprachen

---

## Ausgabe-Flexibilität

### Multi-Channel Publishing
Ein Mapping, mehrere Ausgabekanäle:
```
illusion → Webkatalog
         → PDF-Katalog
         → Amazon / Marktplatz-Feed
         → Google Shopping Feed
         → Händler-Datenblatt
```
Könnte als eigenständiges Produkt vermarktet werden –
viele Unternehmen zahlen teuer für Katalog-Software.

### Statische Seitengenerierung
Fertige HTML-Seiten generieren und auf CDN ausliefern:
- Maximale Performance
- Keine Server-Last zur Laufzeit
- Ähnlich wie Next.js Static Export

---

## Kollaboration & Workflow

### Kommentarfunktion
Redakteur kommentiert direkt an einem Produktfeld:
> „Dieser Text klingt zu technisch – bitte vereinfachen"

### Freigabe-Workflow
Änderungen müssen freigegeben werden bevor sie live gehen.
Für regulierte Branchen (Medizintechnik, Chemie) oft Pflicht.

### Änderungsvergleich
Git-Diff für Produktdaten:
> „Vor der Änderung: 18V / Nach der Änderung: 20V"

---

## Analyse & Optimierung

### Conversion-Tracking
Welche Produktseiten führen zu Anfragen oder Käufen?
Direkt in illusion sichtbar – ohne externes Analytics-Tool.

### SEO-Analyse
Automatische Prüfung pro Produktseite:
- Meta-Title zu lang / zu kurz?
- Fehlt die H1?
- Doppelte Beschreibungen?
- Fehlende Alt-Texte bei Bildern?

### Wettbewerbsvergleich
Preise oder Spezifikationen automatisch mit öffentlich verfügbaren
Konkurrenzdaten abgleichen und im Dashboard anzeigen.

---

## Suche & Elasticsearch

### Elasticsearch-Integration
illusion schreibt bereits gemappte, aufbereitete Daten direkt in Elasticsearch –
keine Rohdaten die erst im Frontend interpretiert werden müssen:

```
bosch.adapter → illusion (gemappte Daten)
                    ↓
             Elasticsearch
                    ↓
          Suchanwendung / Frontend
```

Der Vorteil gegenüber Pimcore/Akeneo: Deren ES-Integration indexiert interne
Rohdaten. illusion indexiert fertig transformierte, kundenspezifische Daten.

### Facetten-Suche
Filterbare Suche nach beliebigen Produkteigenschaften:
- Spannung, Gewicht, Kategorie, Preis
- Facetten werden automatisch aus den MapConfigs abgeleitet
- Kein manuelles Index-Mapping nötig

### Volltextsuche
- Suche über alle konfigurierten Felder
- Mehrsprachig (pro Sprache ein Index)
- Autocomplete / Suggest

### „Ähnliche Produkte"
Elasticsearch More-Like-This direkt aus illusion heraus:
> „Kunden die dieses Produkt angesehen haben, interessierten sich auch für..."

### illusion als Suchplattform
```
illusion
├── Webkatalog-Ausgabe    ← Template-Engine
├── Elasticsearch-Index   ← flexible Suche
├── Solr-Index            ← bereits vorhanden
└── REST API              ← für beliebige Frontends
```
Kein anderes CMS bietet das so integriert und ohne Entwickler konfigurierbar.

---



### Versionierung der Konfiguration
MapConfigs und Templates werden versioniert:
- Rollback auf jeden früheren Stand
- Nachvollziehbarkeit wer was wann geändert hat

### Sandbox / Staging-Umgebung
Änderungen in einer Testumgebung ausprobieren bevor sie live gehen.

### Low-Code Transformationen
Einfache Transformationslogik direkt in der UI schreiben ohne Deployment:
```javascript
// Im UI konfigurierbar – kein Entwickler nötig:
return value.toUpperCase() + " (Professional)";
```

### API-Gateway Funktion
illusion wird zur zentralen Schnittstelle –
alle Drittsysteme fragen hier an, nicht direkt beim Adapter.
Ermöglicht Caching, Authentifizierung und Monitoring an einer Stelle.

### Kafka-Integration (Evolutionsstufe)
Eventgetriebene Architektur für sofortige Propagation von Änderungen:
```
Produkt ändert sich in DB
        ↓
bosch.adapter → Kafka-Event
        ↓           ↓           ↓
    illusion    Solr-Index  Frontend-Cache
    remappt     reindexiert  invalidiert
```
Sinnvoll ab mehreren Abnehmern oder hohem Änderungsvolumen.

---

## Das eine Feature das alles verändert

### „Katalog in 10 Minuten" – Onboarding-Wizard
1. Datenquelle verbinden (URL eingeben)
2. illusion erkennt Felder automatisch
3. Vorgefertigtes Template auswählen
4. Fertig – erster Katalog ist online

Senkt die Time-to-Value dramatisch und macht illusion
selbst verkaufbar ohne aufwändiges Vertriebsgespräch.

---

## Kunden-Experience

### Datenqualitäts-Dashboard (Ersteindruck)
Dashboard das beim ersten Login sofort Mehrwert zeigt:
- Vollständigkeits-Score pro Produkt
- Welche Produkte haben fehlende Bilder?
- Welche Übersetzungen fehlen?
- Wie viele Produkte sind live vs. Draft?

Macht Datenlücken sichtbar die vorher niemand kannte –
erzeugt sofortigen „Aha-Moment" beim Kunden.

### KI-gestützte Änderungsvorschläge
```
illusion erkennt: "Dieses Feld ist bei 847 Produkten leer"
→ Vorschlag: "Automatisch aus Feld X befüllen?"
→ Vorschlag: "Mit DeepL übersetzen?"
```
Kein Wettbewerber macht das heute gut.

### Echtzeit-Collaboration
Mehrere Redakteure arbeiten gleichzeitig am gleichen Produkt –
Änderungen sofort sichtbar ohne Reload, ähnlich wie Google Docs.

---

## Für Entwickler als Zielgruppe

Entwickler sind die Türöffner – nicht der Einkauf entscheidet,
sondern der Entwickler der das System bewertet.

### Developer Experience
- Exzellente Dokumentation mit echten Beispielen
- **Docker Compose** für lokalen Start:
  ```bash
  docker compose up
  # → illusion läuft lokal in 30 Sekunden
  ```
- **OpenAPI/Swagger** für alle Endpoints automatisch generiert
- **Webhooks** für eigene Integrationen

---

## Compliance & Rechtliches

### DSGVO-Konformität
- Welche Felder enthalten personenbezogene Daten? Direkt markierbar
- Audit-Log wer welches Feld wann geändert hat
- Für ISO-Zertifizierungen und regulierte Branchen oft Pflicht

### Internationalisierung über Sprache hinaus
- Unterschiedliche Einheiten je Markt (kg vs. lbs, mm vs. inch)
- Unterschiedliche Preisformate (1.000,00 € vs. 1,000.00 €)
- Rechtliche Pflichtfelder je Land automatisch prüfen

---

## Geschäftsmodell-Features

### White-Label für Agenturen
Agenturen verkaufen illusion unter eigenem Namen:
- Eigenes Logo und Domain
- Eigene Preisgestaltung
- Schnellster Weg zu vielen Kunden ohne eigenen Vertrieb

### Self-Service Onboarding
Kunde registriert sich, verbindet Datenquelle, hat in 10 Minuten einen Katalog –
ohne Salesgespräch, ohne Implementierungsprojekt.
Monatliche Abrechnung nach Produktanzahl oder API-Calls.

---

## Erweiterte technische Features

### Bidirektionales Mapping
Nicht nur Datenbank → Webseite, sondern auch zurückschreiben:
```
Redakteur verbessert Beschreibung in illusion
        ↓
Änderung wird zurück in die Quelldatenbank geschrieben
```
Für Kunden mit schlechter Datenqualität in der Quelldatenbank extrem wertvoll.

### Import-Konfigurator
Nicht nur REST APIs als Datenquelle:
- Excel/CSV Upload mit Feldmapping per Drag & Drop
- Direkter Datenbankzugriff via JDBC
- FTP/SFTP für Unternehmen die noch Dateien übertragen

---

## Das unterschätzte Feature

### Support & Onboarding-Qualität
Pimcore und Akeneo haben schlechten Support für kleine Kunden.
Persönliche Betreuung früh zu investieren schafft loyale Kunden die weiterempfehlen.

> Der erste Kunde der sagt „die haben mir wirklich geholfen"
> ist mehr wert als jede Marketing-Kampagne.
