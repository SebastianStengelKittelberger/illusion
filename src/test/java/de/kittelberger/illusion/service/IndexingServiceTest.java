package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.mapping.JavaCodeMappingHandler;
import de.kittelberger.illusion.mapping.MappingHandler;
import de.kittelberger.illusion.mapping.TextMappingHandler;
import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.DTOType;
import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.ProductMetaData;
import de.kittelberger.illusion.model.TargetType;
import de.kittelberger.illusion.model.AttributeRef;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingServiceTest {

  @Mock
  private LoadDataService loadDataService;

  private IndexingService indexingService;

  @BeforeEach
  void setUp() {
    JavaParserService javaParserService = new JavaParserService();
    List<MappingHandler> handlers = List.of(
      new TextMappingHandler(),
      new JavaCodeMappingHandler(javaParserService)
    );
    indexingService = new IndexingService(loadDataService, handlers);
  }

  // ---------------------------------------------------------------------------
  // Shared MapConfig templates
  // ---------------------------------------------------------------------------

  private static final MapConfig TITLE_SKU_CONFIG = mapConfig(
    TargetType.PRODUCT, DTOType.SKU, "TITLE", "TEXT", "name", "STRING", false);

  private static final MapConfig NAME_PRODUCT_FALLBACK = mapConfig(
    TargetType.PRODUCT, DTOType.PRODUCT, "$NAME$", "TEXT", "name", "STRING", true);

  // ---------------------------------------------------------------------------
  // Return value
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_withNoProducts_returnsEmptyMap() {
    when(loadDataService.getProducts("DE", "de")).thenReturn(List.of());

    Map<String, Pair<String, Object>> result =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG, NAME_PRODUCT_FALLBACK), "DE", "de");

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Service interactions
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_alwaysLoadsProducts() {
    when(loadDataService.getProducts("DE", "de")).thenReturn(List.of());

    indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    org.mockito.Mockito.verify(loadDataService).getProducts("DE", "de");
  }

  // ---------------------------------------------------------------------------
  // MapConfig filtering
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_ignoresCategoryTargetConfigs() {
    MapConfig categoryConfig = mapConfig(TargetType.CATEGORY, DTOType.CATEGORY, "NAME", "TEXT", "name", "STRING", false);
    when(loadDataService.getProducts("DE", "de")).thenReturn(List.of());

    assertThat(indexingService.indexProduct(List.of(categoryConfig), "DE", "de")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Result map content – TEXT/STRING SKU mapping
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_withTitleSkuConfig_andMatchingAttribute_populatesResultMap() {
    Attribute titleAttr = attribute("TITLE", "PRO Screwdriver TX15 x 100 mm");
    Product product = productWithSkuAttributes(List.of(titleAttr), List.of());

    when(loadDataService.getProducts("DE", "de")).thenReturn(List.of(product));

    Map<String, Pair<String, Object>> result =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    assertThat(result).containsKey("name");
    assertThat(result.get("name").getLeft()).isEqualTo("STRING");
    assertThat(result.get("name").getRight()).isEqualTo("PRO Screwdriver TX15 x 100 mm");
  }

  @Test
  void indexProduct_withLocalizedAttribute_extractsCltextValue() {
    Attribute titleAttr = attribute("TITLE", "Schraubendreher TX15 x 100 mm", "Schraubendreher TX15 x 100 mm");
    Product product = productWithSkuAttributes(List.of(titleAttr), List.of());

    when(loadDataService.getProducts("DE", "de")).thenReturn(List.of(product));

    Map<String, Pair<String, Object>> result =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    assertThat(result.get("name").getRight()).isEqualTo("Schraubendreher TX15 x 100 mm");
  }

  @Test
  void indexProduct_withMissingUkeyOnSku_doesNotPopulateResultMap() {
    Attribute otherAttr = attribute("COLOR", "red");
    Product product = productWithSkuAttributes(List.of(otherAttr), List.of());

    when(loadDataService.getProducts("DE", "de")).thenReturn(List.of(product));

    assertThat(indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de")).isEmpty();
  }

  @Test
  void indexProduct_withNameFallback_usesProductMetaDataName() {
    Product product = productWithName("Bohrmaschine XY");

    when(loadDataService.getProducts("DE", "de")).thenReturn(List.of(product));

    Map<String, Pair<String, Object>> result =
      indexingService.indexProduct(List.of(NAME_PRODUCT_FALLBACK), "DE", "de");

    assertThat(result.get("name").getRight()).isEqualTo("Bohrmaschine XY");
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static Attribute attribute(String ukey, String text) {
    return attribute(ukey, text, null);
  }

  private static Attribute attribute(String ukey, String text, String cltext) {
    Attribute a = new Attribute();
    a.setUkey(ukey);
    a.setReferenceIds(Map.of("attrId", 8L));
    Map<String, Object> values = new java.util.HashMap<>();
    values.put("TEXT", text);
    values.put("BOOLEAN", false);
    values.put("CLTEXT", cltext != null ? cltext : text);
    a.setReferences(new AttributeRef(ukey, values));
    return a;
  }

  private static Product productWithSkuAttributes(List<Attribute> skuAttrs, List<Attribute> productAttrs) {
    return new Product(new ProductMetaData("Product", 1L, "ART-001"), List.of(), productAttrs, skuAttrs);
  }

  private static Product productWithName(String name) {
    return new Product(new ProductMetaData(name, 1L, "ART-001"), List.of(), List.of(), List.of());
  }

  private static MapConfig mapConfig(
    TargetType target, DTOType dtoType, String ukey,
    String mappingType, String targetField, String targetFieldType, boolean isFallback
  ) {
    MapConfig config = new MapConfig();
    config.setTarget(target);
    config.setDtoType(dtoType);
    config.setUkey(ukey);
    config.setMappingType(mappingType);
    config.setTargetField(targetField);
    config.setTargetFieldType(targetFieldType);
    config.setIsFallback(isFallback);
    return config;
  }
}