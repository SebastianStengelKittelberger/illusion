package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductVariantMappingHandlerTest {

  private ProductVariantMappingHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ProductVariantMappingHandler();
  }

  // ---------------------------------------------------------------------------
  // supports()
  // ---------------------------------------------------------------------------

  @Test
  void supports_returnsTrueForProductVariantsTargetField() {
    assertThat(handler.supports(config("PRODUCT_VARIANTS"))).isTrue();
  }

  @Test
  void supports_returnsFalseForOtherTargetField() {
    assertThat(handler.supports(config("name"))).isFalse();
  }

  @Test
  void supports_returnsFalseForNullTargetField() {
    MapConfig c = new MapConfig();
    c.setTargetField(null);
    assertThat(handler.supports(c)).isFalse();
  }

  // ---------------------------------------------------------------------------
  // apply()
  // ---------------------------------------------------------------------------

  @Test
  void apply_twoSkus_eachSkuGetsFullVariantSet() {
    SkuAttributes skuAttributes = new SkuAttributes(Map.of(
      "SKU-001", List.of(),
      "SKU-002", List.of()
    ));
    MappingContext ctx = ctx(skuAttributes);
    MapConfig config = config("PRODUCT_VARIANTS");
    config.setTargetFieldType("SET");
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config, ctx, result);

    assertThat(result).containsKeys("SKU-001", "SKU-002");
    @SuppressWarnings("unchecked")
    Set<String> variants001 = (Set<String>) result.get("SKU-001").get("PRODUCT_VARIANTS").getRight();
    assertThat(variants001).containsExactlyInAnyOrder("SKU-001", "SKU-002");

    @SuppressWarnings("unchecked")
    Set<String> variants002 = (Set<String>) result.get("SKU-002").get("PRODUCT_VARIANTS").getRight();
    assertThat(variants002).containsExactlyInAnyOrder("SKU-001", "SKU-002");
  }

  @Test
  void apply_singleSku_variantSetContainsOnlyItself() {
    SkuAttributes skuAttributes = new SkuAttributes(Map.of("SKU-001", List.of()));
    MappingContext ctx = ctx(skuAttributes);
    MapConfig config = config("PRODUCT_VARIANTS");
    config.setTargetFieldType("SET");
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config, ctx, result);

    assertThat(result).containsKey("SKU-001");
    @SuppressWarnings("unchecked")
    Set<String> variants = (Set<String>) result.get("SKU-001").get("PRODUCT_VARIANTS").getRight();
    assertThat(variants).containsExactly("SKU-001");
  }

  @Test
  void apply_mergesIntoExistingSkuEntry() {
    SkuAttributes skuAttributes = new SkuAttributes(Map.of("SKU-001", List.of()));
    MappingContext ctx = ctx(skuAttributes);
    MapConfig config = config("PRODUCT_VARIANTS");
    config.setTargetFieldType("SET");
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();
    result.put("SKU-001", new HashMap<>(Map.of("name", Pair.of("STRING", "Bohrmaschine"))));

    handler.apply(config, ctx, result);

    assertThat(result.get("SKU-001")).containsKeys("name", "PRODUCT_VARIANTS");
    assertThat(result.get("SKU-001").get("name").getRight()).isEqualTo("Bohrmaschine");
  }

  @Test
  void apply_threeSkus_allSkusInEachVariantSet() {
    SkuAttributes skuAttributes = new SkuAttributes(Map.of(
      "SKU-A", List.of(),
      "SKU-B", List.of(),
      "SKU-C", List.of()
    ));
    MappingContext ctx = ctx(skuAttributes);
    MapConfig config = config("PRODUCT_VARIANTS");
    config.setTargetFieldType("SET");
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config, ctx, result);

    assertThat(result).containsKeys("SKU-A", "SKU-B", "SKU-C");
    for (String sku : List.of("SKU-A", "SKU-B", "SKU-C")) {
      @SuppressWarnings("unchecked")
      Set<String> variants = (Set<String>) result.get(sku).get("PRODUCT_VARIANTS").getRight();
      assertThat(variants).containsExactlyInAnyOrder("SKU-A", "SKU-B", "SKU-C");
    }
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static MappingContext ctx(SkuAttributes skuAttributes) {
    Product product = new Product(new ProductMetaData("Test", 1L), List.of(), List.of(), Map.of(), List.of());
    return new MappingContext(skuAttributes, product, null, Map.of(), null, List.of());
  }

  private static MapConfig config(String targetField) {
    MapConfig c = new MapConfig();
    c.setTargetField(targetField);
    return c;
  }
}
