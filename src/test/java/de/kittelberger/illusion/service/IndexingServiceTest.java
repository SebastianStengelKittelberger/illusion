package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.mapping.CategoryMappingContext;
import de.kittelberger.illusion.mapping.CategoryMappingHandler;
import de.kittelberger.illusion.mapping.CategoryTextMappingHandler;
import de.kittelberger.illusion.mapping.JavaCodeMappingHandler;
import de.kittelberger.illusion.mapping.MappingHandler;
import de.kittelberger.illusion.mapping.TextMappingHandler;
import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.Category;
import de.kittelberger.illusion.model.DTOType;
import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.ProductMetaData;
import de.kittelberger.illusion.model.TargetType;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
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
    List<CategoryMappingHandler> categoryHandlers = List.of(new CategoryTextMappingHandler());
    FilterIndexContributor filterIndexContributor = new FilterIndexContributor(javaParserService);
    indexingService = new IndexingService(loadDataService, handlers, categoryHandlers, java.util.Optional.empty(), java.util.Optional.empty(), filterIndexContributor);
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
    mockProducts();

    Map<String, Map<String, Pair<String, Object>>> result =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG, NAME_PRODUCT_FALLBACK), "DE", "de");

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Service interactions
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_alwaysLoadsProducts() {
    mockProducts();

    indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    verify(loadDataService).streamProducts(org.mockito.ArgumentMatchers.eq("DE"), org.mockito.ArgumentMatchers.eq("de"), org.mockito.ArgumentMatchers.any());
  }

  // ---------------------------------------------------------------------------
  // MapConfig filtering
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_ignoresCategoryTargetConfigs() {
    MapConfig categoryConfig = mapConfig(TargetType.CATEGORY, DTOType.CATEGORY, "NAME", "TEXT", "name", "STRING", false);
    mockProducts();

    assertThat(indexingService.indexProduct(List.of(categoryConfig), "DE", "de")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Result map content – TEXT/STRING SKU mapping
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_withTitleSkuConfig_andMatchingAttribute_populatesResultMap() {
    Attribute titleAttr = attribute("TITLE", "PRO Screwdriver TX15 x 100 mm");
    Product product = productWithSkuAttributes(List.of(titleAttr), List.of());

    mockProducts(product);

    Map<String, Map<String, Pair<String, Object>>> result =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    assertThat(result).hasSize(1);
    assertThat(result.get("SKU-001")).containsKey("name");
    assertThat(result.get("SKU-001").get("name").getLeft()).isEqualTo("STRING");
    assertThat(result.get("SKU-001").get("name").getRight()).isEqualTo("PRO Screwdriver TX15 x 100 mm");
  }

  @Test
  void indexProduct_withLocalizedAttribute_extractsCltextValue() {
    Attribute titleAttr = attribute("TITLE", "Schraubendreher TX15 x 100 mm", "Schraubendreher TX15 x 100 mm");
    Product product = productWithSkuAttributes(List.of(titleAttr), List.of());

    mockProducts(product);

    Map<String, Map<String, Pair<String, Object>>> result =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    assertThat(result.get("SKU-001").get("name").getRight()).isEqualTo("Schraubendreher TX15 x 100 mm");
  }

  @Test
  void indexProduct_withMissingUkeyOnSku_doesNotPopulateResultMap() {
    Attribute otherAttr = attribute("COLOR", "red");
    Product product = productWithSkuAttributes(List.of(otherAttr), List.of());

    mockProducts(product);

    assertThat(indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de").get("SKU-001")).isNullOrEmpty();
  }

  @Test
  void indexProduct_withNameFallback_usesProductMetaDataName() {
    Product product = productWithName("Bohrmaschine XY");

    mockProducts(product);

    Map<String, Map<String, Pair<String, Object>>> result =
      indexingService.indexProduct(List.of(NAME_PRODUCT_FALLBACK), "DE", "de");

    assertThat(result.get("SKU-001").get("name").getRight()).isEqualTo("Bohrmaschine XY");
  }

  // ---------------------------------------------------------------------------
  // indexCategories
  // ---------------------------------------------------------------------------

  @Test
  void indexCategories_withNoCategories_returnsEmptyMap() {
    when(loadDataService.getCategories("DE", "de")).thenReturn(List.of());

    Map<String, Map<String, Pair<String, Object>>> result =
      indexingService.indexCategories(List.of(categoryConfig("TITLE")), "DE", "de");

    assertThat(result).isEmpty();
  }

  @Test
  void indexCategories_mapsTextAttributeForMatchingUkey() {
    Category cat = category("CAT-001", "TITLE", "Elektrowerkzeuge");
    when(loadDataService.getCategories("DE", "de")).thenReturn(List.of(cat));

    Map<String, Map<String, Pair<String, Object>>> result =
      indexingService.indexCategories(List.of(categoryConfig("TITLE")), "DE", "de");

    assertThat(result).containsKey("CAT-001");
    assertThat(result.get("CAT-001").get("name").getRight()).isEqualTo("Elektrowerkzeuge");
  }

  @Test
  void indexCategories_keysResultByCategoryUkey() {
    Category cat1 = category("CAT-001", "TITLE", "Elektrowerkzeuge");
    Category cat2 = category("CAT-002", "TITLE", "Handwerkzeuge");
    when(loadDataService.getCategories("DE", "de")).thenReturn(List.of(cat1, cat2));

    Map<String, Map<String, Pair<String, Object>>> result =
      indexingService.indexCategories(List.of(categoryConfig("TITLE")), "DE", "de");

    assertThat(result).containsKeys("CAT-001", "CAT-002");
  }

  @Test
  void indexCategories_filtersOutNonCategoryConfigs() {
    Category cat = category("CAT-001", "TITLE", "Elektrowerkzeuge");
    when(loadDataService.getCategories("DE", "de")).thenReturn(List.of(cat));

    // Pass only PRODUCT-target config — no mapped fields, only skus/parentId are always added
    Map<String, Map<String, Pair<String, Object>>> result =
      indexingService.indexCategories(List.of(TITLE_SKU_CONFIG), "DE", "de");

    assertThat(result).containsKey("CAT-001");
    assertThat(result.get("CAT-001")).containsOnlyKeys("skus");
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
    a.setReferences(values);
    return a;
  }

  private static Product productWithSkuAttributes(List<Attribute> skuAttrs, List<Attribute> productAttrs) {
    return new Product(new ProductMetaData("Product", 1L, "ART-001"), List.of(), productAttrs, Map.of("SKU-001", skuAttrs), List.of());
  }

  private static Product productWithName(String name) {
    return new Product(new ProductMetaData(name, 1L, "ART-001"), List.of(), List.of(), Map.of("SKU-001", List.of()), List.of());
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

  @SafeVarargs
  private final void mockProducts(Product... products) {
    doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      Predicate<Product> consumer = invocation.getArgument(2, Predicate.class);
      for (Product product : products) {
        if (!consumer.test(product)) {
          break;
        }
      }
      return null;
    }).when(loadDataService).streamProducts(org.mockito.ArgumentMatchers.eq("DE"), org.mockito.ArgumentMatchers.eq("de"), org.mockito.ArgumentMatchers.any());

    when(loadDataService.getMediaObjects("DE", "de")).thenReturn(Map.of());
    when(loadDataService.getDomain("DE", "de")).thenReturn("https://example.test");
  }

  private static MapConfig categoryConfig(String ukey) {
    MapConfig c = new MapConfig();
    c.setTarget(TargetType.CATEGORY);
    c.setDtoType(DTOType.CATEGORY);
    c.setMappingType("TEXT");
    c.setTargetFieldType("STRING");
    c.setUkey(ukey);
    c.setTargetField("name");
    return c;
  }

  private static Category category(String ukey, String attrUkey, String textValue) {
    Attribute a = new Attribute();
    a.setUkey(attrUkey);
    a.setReferences(Map.of("TEXT", textValue, "BOOLEAN", false));
    return new Category(1L, ukey, null, List.of(), Map.of(attrUkey, List.of(a)));
  }
}
