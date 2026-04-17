# PRD – Kategorie-Mapping: bosch.adapter → Illusion → Moonlight

**Status**: Entwurf  
**Erstellt**: April 2026  
**Scope**: `bosch.adapter` + `illusion` + `moonlight` + `summerlight`

---

## Problem

Kategorien sind bisher kein Teil des Rendering-Stacks. Im bosch.adapter werden `CategoryDTO`-Objekte aus den XML-Dateien geladen, aber nicht weiterverarbeitet. Illusion hat `CATEGORY` als `TargetType` und `DTOType` reserviert, aber keine vollständige Implementierung dahinter. Moonlight rendert ausschließlich Produkte; Kategorieseiten sind nicht ansteuerbar.

**Ziel**: Kategorien sollen als eigenständige Dokumente gemappt und gerendert werden – **getrennt vom Produkt-Mapping**. Der vollständige Datenfluss lautet:

```
XML (category-*.xml)
  → bosch.adapter: CategoryDTO → Category → bosch-categories-{country}-{language}
  → illusion: Category laden, mappen → illusion-categories-{country}-{language}
  → moonlight: Kategorieseiten rendern
```

---

## Datengrundlage

### Im bosch.adapter (XML-Quelle)

`CategoryDTO` (bereits vorhanden) enthält:
- `ukey` – eindeutiger Kategorieschlüssel (Dokumenten-ID in ES)
- `id` – numerische ID
- `attrvals` – Attributwerte der Kategorie (`Val` mit `List<Attrval>`, jede mit `ukey`)
- `products` → `List<Category$Products$Product>` → jedes Element trägt `id` (BigInteger = Product-ID)

`ProductDTO` (bereits vorhanden) enthält:
- `id` – Product-ID
- `skus.sku[].sku` – SKU-Codes der dem Produkt zugeordneten Varianten

**Auflösungslogik SKUs:**  
`categoryDTO.products.product[].id` → Product-IDs → `productDTO.skus.sku[].sku` → SKU-Codes

**Auflösungslogik Attribute:**  
`categoryDTO.attrvals.attrval[]` → analog zur Produktattribut-Abbildung in `MapProductDTOService.mapProductAttributes()` → `Map<ukey, List<Attribute>>`

---

## User Stories

**US-01 — Kategorien in Elasticsearch indexieren (bosch.adapter)**  
Als Betreiber möchte ich Kategorien via `POST /{country}/{language}/index/categories` in einen dedizierten ES-Index schreiben (Trigger am bestehenden `BoschController`), damit Illusion die Daten laden kann.

**US-02 — Kategorien in Illusion laden**  
Als Illusion-Betreiber möchte ich Kategorien aus ES laden und in einen eigenen `CategoryMappingContext` überführen, damit Kategorie-Mapping-Handler darauf arbeiten können.

**US-01 — Kategorien in Elasticsearch indexieren (bosch.adapter)**  
Als Betreiber möchte ich Kategorien via `POST /{country}/{language}/index/categories` in einen dedizierten ES-Index schreiben, damit Illusion die Daten laden kann.

**US-02 — Kategorien in Illusion laden und mappen**  
Als Illusion-Betreiber möchte ich Kategorien aus ES laden, mappen und in einen eigenen `illusion-categories-`-Index schreiben, damit Moonlight die Daten abrufen kann.

**US-03 — Kategorie-Mapping separat anstoßen**  
Als technischer Redakteur möchte ich `POST /{country}/{language}/index/categories` (am bestehenden `IndexController`) mit einer Liste von `MapConfig`s aufrufen, um Kategorien **unabhängig** vom Produkt-Lauf zu mappen und zu indexieren.

**US-04 — Kategorie-Attribute und SKUs mappen**  
Als technischer Redakteur möchte ich `MapConfig`-Einträge mit `dtoType: CATEGORY` und `target: CATEGORY` anlegen, die Kategorie-Attribute per `ukey` ansprechen – analog zur SKU-Attribut-Konfiguration.

**US-05 — Kategorieseiten in Moonlight rendern**  
Als Endnutzer möchte ich Kategorieseiten über konfigurierbare URLs aufrufen können, die Moonlight mit gemappten Kategorie-Daten aus Illusion rendert.

**US-06 — Kategorierouten konfigurieren (Summerlight)**  
Als technischer Redakteur möchte ich in Summerlight Routen vom Typ `CATEGORY_PAGE` anlegen können, die einen `{categoryUkey}`-Platzhalter unterstützen.

---

## Lösungsdesign

### bosch.adapter ✅ (bereits implementiert)

#### 1. Modell: `Category`

```java
public record Category(
  Long id,
  String ukey,
  Long parentId,
  List<String> skus,
  Map<String, List<Attribute>> attributes   // attrval-ukey → Attributwerte
) {}
```

#### 2. `MapCategoryDTOService`

- Lädt alle `CategoryDTO`s via `LoadDataService.getCategoryDTOs()`
- Baut Lookup `productId → List<String> skus` aus **allen** `ProductDTO`s (`getAllProductDTOs()`, ohne `xml.product-ids`-Filter)
- Mappt `attrvals` → `Map<String, List<Attribute>>`
- Gibt `List<Category>` zurück

#### 3. `ElasticsearchCategoryIndexService`

- Index-Name: `bosch-categories-{country}-{language}`
- Dokument-ID = `category.ukey()`
- Trigger: `POST /{country}/{language}/index/categories` → `CategoryController`

---

### illusion ✅ (bereits implementiert) + ⚠️ Ergänzung nötig

#### 4. Modell: `Category` ✅

```java
public record Category(
  Long id,
  String ukey,
  Long parentId,
  List<String> skus,
  Map<String, List<Attribute>> attributes
) {}
```

#### 5. `ElasticsearchCategoryLoadService` ✅

- `@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")`
- Liest aus `bosch-categories-{country}-{language}`
- Gibt `List<Category>` zurück

#### 6. `CategoryMappingHandler` + `CategoryTextMappingHandler` ✅

Eigenes Interface (nicht `MappingHandler`), da der Kontext `CategoryMappingContext` statt `MappingContext` verwendet. Erste Implementierung: TEXT/STRING-Mapping analog zu `TextMappingHandler`.

#### 7. `IndexingService.indexCategories()` ✅ / ⚠️

Vorhanden, ruft aber aktuell `ElasticsearchIndexService.indexResults()` auf → schreibt in den **Produkt-Index** `illusion-{country}-{language}` und fügt fälschlicherweise ein `"sku"`-Feld mit dem Kategorie-ukey ein.

**Nötige Ergänzung**: Neuer `ElasticsearchCategoryResultIndexService` in illusion:

```java
@Service
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchCategoryResultIndexService {

  @Value("${elasticsearch.category-result-index-prefix:illusion-categories}")
  private String indexPrefix;

  // indexResults(results, country, language):
  //   → Index: illusion-categories-{country}-{language}
  //   → Dokument-ID: categoryUkey
  //   → Kein "sku"-Feld; stattdessen "ukey": categoryUkey
}
```

`IndexingService` erhält `Optional<ElasticsearchCategoryResultIndexService>` als zusätzliche Dependency und ruft diese statt des allgemeinen `ElasticsearchIndexService` in `indexCategories()` auf.

**Trigger**: `POST /{country}/{language}/index/categories` am bestehenden `IndexController` ✅

---

### moonlight 🆕

Moonlight's `DataService` liest bereits aus `illusion-{country}-{language}` per `GET /_doc/{id}`. Für Kategorien wird derselbe Mechanismus genutzt, aber gegen `illusion-categories-{country}-{language}`.

#### 8. `DataService` erweitern

```java
@Cacheable(value = "category-data", key = "#country + '-' + #language + '-' + #categoryUkey")
public Map<String, Map<String, Object>> fetchCategoryData(
    String country, String language,
    String categoryUkey, List<MapConfig> mapConfigs
) {
    // Index: illusion-categories-{country}-{language}
    // GET /{indexName}/_doc/{categoryUkey}
    // Filtert mapConfigs auf target == CATEGORY
    // Gleiche Wrap-Logik wie fetchData(): fieldName → {targetFieldType → value}
}
```

Zweiter Cache-Name `category-data` in `application.yaml` eintragen (30s TTL, analog zu `product-data`).

#### 9. Neuer `CategoryController`

```java
@GetMapping("/{country}/{language}/category-{categoryUkey}")
public String categoryHandler(
    @PathVariable String country,
    @PathVariable String language,
    @PathVariable String categoryUkey,
    @RequestParam(defaultValue = "kategorienseite") String page,
    @RequestParam(defaultValue = "false") boolean editMode
) {
    return renderService.renderCategoryPage(country, language, categoryUkey, page, editMode);
}
```

Analog zu `ProductController`. Der direkte URL-Prefix (`/category-`) hat Vorrang vor dem `/**`-Catch-all in `RoutingController`.

#### 10. `RouteConfig.PageType` erweitern

```java
public enum PageType {
    PRODUCT_PAGE,
    CMS_PAGE,
    CATEGORY_PAGE   // NEU
}
```

#### 11. `RoutingController.catchAll()` erweitern

```java
case CATEGORY_PAGE -> {
    Map<String, String> vars = routingStorageService.extractPathVariables(route.getUrl(), path);
    String categoryUkey = vars.getOrDefault("categoryUkey", "EXAMPLE");
    yield renderService.renderCategoryPage(country, language, categoryUkey, route.getPageName(), editMode);
}
```

Ermöglicht konfigurierbare Kategorierouten wie `/kategorien/{categoryUkey}` oder `/c/{categoryUkey}`.

#### 12. `RenderService.renderCategoryPage()` (neu)

```java
public String renderCategoryPage(
    String country, String language,
    String categoryUkey, String pageName, boolean editMode
) {
    TemplateProperties properties = templateStorageService.loadPage(country, language, pageName);
    List<MapConfig> mapConfig = dataService.loadIllusionMappingConfig(country, language)
        .stream()
        .filter(c -> TargetType.CATEGORY.name().equals(c.getTarget() != null ? c.getTarget().name() : null))
        .toList();
    Map<String, String> globalLabels = templateStorageService.loadLabels(country, language);
    // merge page-level labels …
    Map<String, Map<String, Object>> data =
        dataService.fetchCategoryData(country, language, categoryUkey, mapConfig);
    // renderSlotBasedPage / renderTemplate — identisch zu Produkt-Rendering
}
```

Die Template-Notation `$skuAttr(UKEY)$.getText()` funktioniert unverändert, da sie nur auf `dataMap[fieldName]` zugreift – der Datentyp (Produkt oder Kategorie) ist für die Thymeleaf-Auflösung irrelevant.

---

### summerlight 🆕

#### 13. TypeScript-Typen erweitern

In `src/types/index.ts`:

```typescript
// Bereits vorhanden:
export type DTOType = 'PRODUCT' | 'SKU' | 'CATEGORY';
export type TargetType = 'PRODUCT' | 'CATEGORY';

// Erweitern:
export type PageType = 'PRODUCT_PAGE' | 'CMS_PAGE' | 'CATEGORY_PAGE';  // NEU: CATEGORY_PAGE
```

#### 14. `RoutingEditor` erweitern

- `CATEGORY_PAGE` als Option im `pageType`-Dropdown anzeigen
- Hinweis: URL-Pattern sollte `{categoryUkey}` als Platzhalter unterstützen (analog zu `{sku}` bei Produkten)
- Keine Änderung am Editor-Logik nötig, da `extractPathVariables()` bereits generisch arbeitet

Keine Änderungen am `MapConfigEditor` nötig – `dtoType: CATEGORY` und `target: CATEGORY` sind bereits unterstützt.

---

## Vollständiger Datenfluss (End-to-End)

```
XML (category-*.xml)
  └─ bosch.adapter: LoadDataService.getCategoryDTOs() + getAllProductDTOs()
  └─ MapCategoryDTOService → Category { id, ukey, parentId, skus, attributes }
  └─ ElasticsearchCategoryIndexService → bosch-categories-de-de
       Trigger: POST /{country}/{language}/index/categories @ CategoryController

bosch-categories-de-de
  └─ illusion: ElasticsearchCategoryLoadService → List<Category>
  └─ IndexingService.indexCategories()
       └─ CategoryMappingContext { category, locale }
       └─ CategoryMappingHandler.apply() (z.B. CategoryTextMappingHandler)
       └─ ElasticsearchCategoryResultIndexService → illusion-categories-de-de
       Trigger: POST /{country}/{language}/index/categories @ IndexController

illusion-categories-de-de
  └─ moonlight: DataService.fetchCategoryData(categoryUkey)
  └─ RenderService.renderCategoryPage()
       └─ TemplateProperties (aus ES: moonlight-pages)
       └─ Slot-Rendering mit $skuAttr(UKEY)$.getText()-Pattern
       └─ Thymeleaf → gerendertes HTML
       Trigger: GET /{country}/{language}/category-{categoryUkey}
              oder konfigurierbarer Route via RoutingController
```

---

## ES-Indizes Übersicht

| Index | Befüllt von | Gelesen von | Inhalt |
|-------|-------------|-------------|--------|
| `bosch-categories-{c}-{l}` | bosch.adapter | illusion | Rohe CategoryDTOs als Category-Objekte |
| `illusion-categories-{c}-{l}` | illusion | moonlight | Gemappte Kategorifelder (targetField → value) |
| `illusion-{c}-{l}` | illusion | moonlight | Gemappte Produktfelder (SKU-Dokumente) |

---

## Implementierungsstand

| Komponente | Status |
|-----------|--------|
| bosch.adapter: `Category`, `MapCategoryDTOService`, `ElasticsearchCategoryIndexService`, `CategoryController` | ✅ Fertig |
| illusion: `Category`, `CategoryMappingContext`, `CategoryMappingHandler`, `CategoryTextMappingHandler` | ✅ Fertig |
| illusion: `ElasticsearchCategoryLoadService`, `LoadDataService.getCategories()` | ✅ Fertig |
| illusion: `IndexingService.indexCategories()`, `IndexController`-Endpunkt | ✅ Fertig |
| illusion: `ElasticsearchCategoryResultIndexService` (eigener Index für gemappte Kategorien) | ❌ Fehlt |
| moonlight: `DataService.fetchCategoryData()` | ❌ Fehlt |
| moonlight: `CategoryController` | ❌ Fehlt |
| moonlight: `RouteConfig.PageType.CATEGORY_PAGE` | ❌ Fehlt |
| moonlight: `RoutingController` – CATEGORY_PAGE-Zweig | ❌ Fehlt |
| moonlight: `RenderService.renderCategoryPage()` | ❌ Fehlt |
| summerlight: `PageType`-Erweiterung + RoutingEditor | ❌ Fehlt |

---

## Nicht im Scope (dieser Iteration)

- Kategorie-Bilder (`mediaobjects`)
- Filter-Konfiguration für Kategorien (`FilterConfig`)
- Edit-Mode-Annotierungen für Kategorie-Felder in Moonlight
- Verknüpfung von Kategorien mit dem Produkt-`MappingContext`
