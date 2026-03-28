package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComplexMappingHandlerTest {

  private ComplexMappingHandler handler;

  private static final String SKU = "SKU-001";

  @BeforeEach
  void setUp() {
    handler = new ComplexMappingHandler();
  }

  // ---------------------------------------------------------------------------
  // supports()
  // ---------------------------------------------------------------------------

  @Test
  void supports_returnsTrueWhenComplexMappingIsSet() {
    MapConfig config = new MapConfig();
    config.setComplexMapping(new ComplexMapping());
    assertThat(handler.supports(config)).isTrue();
  }

  @Test
  void supports_returnsFalseWhenComplexMappingIsNull() {
    MapConfig config = new MapConfig();
    config.setComplexMapping(null);
    assertThat(handler.supports(config)).isFalse();
  }

  // ---------------------------------------------------------------------------
  // apply() – happy path
  // ---------------------------------------------------------------------------

  @Test
  void apply_attributeMatchingReferencedAttrClass_isMappedToResult() {
    // Reference whose attrClasses.right contains ATTR_CLASS_UKEY, and left is the attribute's ukey
    Reference ref = new Reference(1L, "ref-ukey", "ref-name",
      new AttrClassRef("FEATURE_UKEY", List.of(new AttrClass("FeatureClass", "ATTR_CLASS_UKEY"))));

    Attribute attr = attr("FEATURE_UKEY", "FeatureValue");
    MappingContext ctx = ctx(SKU, attr, List.of(ref));

    MapConfig config = complexConfig(List.of("ATTR_CLASS_UKEY"), "features", "OBJECT");
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config, ctx, result);

    assertThat(result).containsKey(SKU);
    assertThat(result.get(SKU)).containsKey("features");
    @SuppressWarnings("unchecked")
    Map<String, Object> featureMap = (Map<String, Object>) result.get(SKU).get("features").getRight();
    assertThat(featureMap).containsEntry("FEATURE_UKEY", "FeatureValue");
  }

  @Test
  void apply_attributeNotMatchingAnyReference_isExcluded() {
    // Reference matches ATTR_CLASS_UKEY, but attribute has a different ukey
    Reference ref = new Reference(1L, "ref-ukey", "ref-name",
      new AttrClassRef("FEATURE_UKEY", List.of(new AttrClass("FeatureClass", "ATTR_CLASS_UKEY"))));

    Attribute attr = attr("UNRELATED_UKEY", "SomeValue");
    MappingContext ctx = ctx(SKU, attr, List.of(ref));

    MapConfig config = complexConfig(List.of("ATTR_CLASS_UKEY"), "features", "OBJECT");
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config, ctx, result);

    // The attribute does not match any reference's left ukey → result may be empty or have an empty map
    if (result.containsKey(SKU) && result.get(SKU).containsKey("features")) {
      @SuppressWarnings("unchecked")
      Map<String, Object> featureMap = (Map<String, Object>) result.get(SKU).get("features").getRight();
      assertThat(featureMap).doesNotContainKey("UNRELATED_UKEY");
    } else {
      assertThat(result).doesNotContainKey(SKU);
    }
  }

  @Test
  void apply_noMatchingReferencedAttrClass_producesNoResult() {
    // Reference's attrClasses.right does NOT include the configured ATTR_CLASS_UKEY
    Reference ref = new Reference(1L, "ref-ukey", "ref-name",
      new AttrClassRef("FEATURE_UKEY", List.of(new AttrClass("OtherClass", "OTHER_UKEY"))));

    Attribute attr = attr("FEATURE_UKEY", "FeatureValue");
    MappingContext ctx = ctx(SKU, attr, List.of(ref));

    MapConfig config = complexConfig(List.of("ATTR_CLASS_UKEY"), "features", "OBJECT");
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config, ctx, result);

    assertThat(result).doesNotContainKey(SKU);
  }

  @Test
  void apply_attributeWithNullReferences_mapsToNullValue() {
    Reference ref = new Reference(1L, "ref-ukey", "ref-name",
      new AttrClassRef("FEAT", List.of(new AttrClass("AC", "AC_UKEY"))));

    Attribute attr = new Attribute();
    attr.setUkey("FEAT");
    attr.setReferences(null); // no text → extractText returns null

    MappingContext ctx = ctx(SKU, attr, List.of(ref));
    MapConfig config = complexConfig(List.of("AC_UKEY"), "features", "OBJECT");
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config, ctx, result);

    assertThat(result).containsKey(SKU);
    @SuppressWarnings("unchecked")
    Map<String, Object> featureMap = (Map<String, Object>) result.get(SKU).get("features").getRight();
    assertThat(featureMap).containsEntry("FEAT", null);
  }

  @Test
  void apply_mergesIntoExistingSkuEntry() {
    Reference ref = new Reference(1L, "ref-ukey", "ref-name",
      new AttrClassRef("FEAT2", List.of(new AttrClass("AC", "AC_UKEY"))));

    Attribute attr = attr("FEAT2", "Value2");
    MappingContext ctx = ctx(SKU, attr, List.of(ref));

    MapConfig config = complexConfig(List.of("AC_UKEY"), "features", "OBJECT");
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();
    // Pre-populate with an existing feature map for this SKU
    Map<String, Object> existingFeatures = new HashMap<>();
    existingFeatures.put("FEAT1", "Value1");
    result.put(SKU, new HashMap<>(Map.of("features", Pair.of("OBJECT", existingFeatures))));

    handler.apply(config, ctx, result);

    @SuppressWarnings("unchecked")
    Map<String, Object> featureMap = (Map<String, Object>) result.get(SKU).get("features").getRight();
    assertThat(featureMap).containsEntry("FEAT1", "Value1").containsEntry("FEAT2", "Value2");
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static MappingContext ctx(String sku, Attribute attribute, List<Reference> referenceList) {
    // Use ArrayList so ComplexMappingHandler can call removeIf on the list
    SkuAttributes skuAttributes = new SkuAttributes(new java.util.HashMap<>(Map.of(sku, new java.util.ArrayList<>(List.of(attribute)))));
    Product product = new Product(new ProductMetaData("Test", 1L), List.of(), List.of(),
      new java.util.HashMap<>(Map.of(sku, new java.util.ArrayList<>(List.of(attribute)))), List.of());
    return new MappingContext(skuAttributes, product, null, Map.of(), null, referenceList);
  }

  private static Attribute attr(String ukey, String text) {
    Attribute a = new Attribute();
    a.setUkey(ukey);
    Map<String, Object> refs = new HashMap<>();
    refs.put("TEXT", text);
    a.setReferences(refs);
    return a;
  }

  private static MapConfig complexConfig(List<String> referencedAttrClasses, String targetField,
      String targetFieldType) {
    ComplexMapping complexMapping = new ComplexMapping();
    complexMapping.setReferencedAttrClasses(referencedAttrClasses);
    MapConfig c = new MapConfig();
    c.setComplexMapping(complexMapping);
    c.setTargetField(targetField);
    c.setTargetFieldType(targetFieldType);
    return c;
  }
}
