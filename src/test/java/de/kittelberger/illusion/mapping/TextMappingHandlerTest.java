package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.ProductMetaData;
import de.kittelberger.illusion.model.SkuAttributes;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TextMappingHandlerTest {

  private TextMappingHandler handler;

  private static final String SKU = "SKU-001";

  @BeforeEach
  void setUp() {
    handler = new TextMappingHandler();
  }

  // ---------------------------------------------------------------------------
  // supports()
  // ---------------------------------------------------------------------------

  @Test
  void supports_returnsTrueForTEXTMappingType() {
    assertThat(handler.supports(config("TEXT", "STRING"))).isTrue();
  }

  @Test
  void supports_returnsFalseForIMAGEMappingType() {
    assertThat(handler.supports(config("IMAGE", "IMAGE"))).isFalse();
  }

  @Test
  void supports_returnsFalseForCustomMappingType() {
    assertThat(handler.supports(config("CUSTOM", "STRING"))).isFalse();
  }

  // ---------------------------------------------------------------------------
  // apply() – TEXT / CLTEXT extraction
  // ---------------------------------------------------------------------------

  @Test
  void apply_extractsTextValue() {
    MappingContext ctx = ctx(SKU, attr("TITLE", "Bohrmaschine", null), null);
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("TEXT", "STRING", "TITLE", "name", false), ctx, result);

    assertThat(result.get(SKU).get("name").getRight()).isEqualTo("Bohrmaschine");
  }

  @Test
  void apply_prefersCltextOverText() {
    MappingContext ctx = ctx(SKU, attr("TITLE", "English title", "Deutscher Titel"), null);
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("TEXT", "STRING", "TITLE", "name", false), ctx, result);

    assertThat(result.get(SKU).get("name").getRight()).isEqualTo("Deutscher Titel");
  }

  @Test
  void apply_returnsNullWhenNoReferences() {
    Attribute noRefs = new Attribute();
    noRefs.setUkey("TITLE");
    noRefs.setReferences(null);

    MappingContext ctx = ctx(SKU, noRefs, null);
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("TEXT", "STRING", "TITLE", "name", false), ctx, result);

    assertThat(result.get(SKU).get("name").getRight()).isNull();
  }

  @Test
  void apply_skipsWhenTargetFieldTypeIsNotString() {
    MappingContext ctx = ctx(SKU, attr("TITLE", "Tool", null), null);
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("TEXT", "IMAGE", "TITLE", "name", false), ctx, result);

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // apply() – isFallback
  // ---------------------------------------------------------------------------

  @Test
  void apply_isFallbackSkipsWhenFieldAlreadyPopulated() {
    MappingContext ctx = ctx(SKU, attr("TITLE", "Fallback Value", null), null);
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();
    result.put("name", Map.of("name", Pair.of("STRING", "Existing Value")));

    handler.apply(config("TEXT", "STRING", "TITLE", "name", true), ctx, result);

    // The fallback check uses config.getTargetField(), which is "name" — it already exists in result
    // so the handler should not overwrite for the SKU either
    assertThat(result.get(SKU)).isNullOrEmpty();
  }

  @Test
  void apply_isFallbackWritesWhenFieldNotYetPopulated() {
    MappingContext ctx = ctx(SKU, attr("TITLE", "Fallback Value", null), null);
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("TEXT", "STRING", "TITLE", "name", true), ctx, result);

    assertThat(result.get(SKU).get("name").getRight()).isEqualTo("Fallback Value");
  }

  // ---------------------------------------------------------------------------
  // apply() – $NAME$ placeholder
  // ---------------------------------------------------------------------------

  @Test
  void apply_usesProductMetaDataNameForNamePlaceholder_orNullWhenMetaDataMissing() {
    SkuAttributes skuAttributes = new SkuAttributes(Map.of(SKU, List.of()));
    MapConfig nameConfig = config("TEXT", "STRING", "$NAME$", "name", false);

    Product withName = new Product(new ProductMetaData("Bohrmaschine GSB 18V", 1L), List.of(), List.of(),
      Map.of(SKU, List.of()), List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();
    handler.apply(nameConfig, new MappingContext(skuAttributes, withName, null, Map.of(), null, List.of()), result);
    assertThat(result.get(SKU).get("name").getRight()).isEqualTo("Bohrmaschine GSB 18V");

    Product withoutMeta = new Product(null, List.of(), List.of(), Map.of(SKU, List.of()), List.of());
    result.clear();
    handler.apply(nameConfig, new MappingContext(skuAttributes, withoutMeta, null, Map.of(), null, List.of()), result);
    assertThat(result.get(SKU).get("name").getRight()).isNull();
  }

  // ---------------------------------------------------------------------------
  // apply() – merges into existing SKU result entry
  // ---------------------------------------------------------------------------

  @Test
  void apply_mergesIntoExistingSkuEntry() {
    MappingContext ctx = ctx(SKU, attr("COLOR", "red", null), null);
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();
    result.put(SKU, new HashMap<>(Map.of("name", Pair.of("STRING", "Bohrmaschine"))));

    handler.apply(config("TEXT", "STRING", "COLOR", "color", false), ctx, result);

    assertThat(result.get(SKU)).containsKeys("name", "color");
    assertThat(result.get(SKU).get("color").getRight()).isEqualTo("red");
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static MappingContext ctx(String sku, Attribute attribute, Product product) {
    SkuAttributes skuAttributes = new SkuAttributes(Map.of(sku, List.of(attribute)));
    if (product == null) {
      product = new Product(new ProductMetaData("Test Product", 1L), List.of(), List.of(),
        Map.of(sku, List.of(attribute)), List.of());
    }
    return new MappingContext(skuAttributes, product, null, Map.of(), null, List.of());
  }

  private static Attribute attr(String ukey, String text, String cltext) {
    Attribute a = new Attribute();
    a.setUkey(ukey);
    Map<String, Object> refs = new HashMap<>();
    refs.put("TEXT", text);
    if (cltext != null) refs.put("CLTEXT", cltext);
    a.setReferences(refs);
    return a;
  }

  private static MapConfig config(String mappingType, String targetFieldType) {
    return config(mappingType, targetFieldType, "TITLE", "name", false);
  }

  private static MapConfig config(String mappingType, String targetFieldType, String ukey, String targetField, boolean isFallback) {
    MapConfig c = new MapConfig();
    c.setMappingType(mappingType);
    c.setTargetFieldType(targetFieldType);
    c.setUkey(ukey);
    c.setTargetField(targetField);
    c.setIsFallback(isFallback);
    return c;
  }
}
