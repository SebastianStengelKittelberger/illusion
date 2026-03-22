package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.ProductMetaData;
import de.kittelberger.illusion.model.SkuAttributes;
import de.kittelberger.illusion.service.JavaParserService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JavaCodeMappingHandlerTest {

  private JavaCodeMappingHandler handler;

  private static final String SKU = "SKU-001";

  @BeforeEach
  void setUp() {
    handler = new JavaCodeMappingHandler(new JavaParserService());
  }

  // ---------------------------------------------------------------------------
  // supports()
  // ---------------------------------------------------------------------------

  @Test
  void supports_returnsTrueWhenJavaCodeIsPresent() {
    assertThat(handler.supports(configWithCode("'hello'", "CUSTOM"))).isTrue();
  }

  @Test
  void supports_returnsFalseWhenJavaCodeIsNullOrBlank() {
    assertThat(handler.supports(configWithNullCode())).isFalse();

    MapConfig blank = new MapConfig();
    blank.setJavaCode("   ");
    assertThat(handler.supports(blank)).isFalse();

    MapConfig empty = new MapConfig();
    empty.setJavaCode("");
    assertThat(handler.supports(empty)).isFalse();
  }

  // ---------------------------------------------------------------------------
  // apply() – literal SpEL expressions
  // ---------------------------------------------------------------------------

  @Test
  void apply_evaluatesStringLiteralExpression() {
    MappingContext ctx = ctx(SKU, List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(configWithCode("'static value'", "STRING"), ctx, result);

    assertThat(result.get(SKU).get("computed").getRight()).isEqualTo("static value");
  }

  @Test
  void apply_evaluatesArithmeticExpression() {
    MappingContext ctx = ctx(SKU, List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(configWithCode("6 * 7", "NUMBER"), ctx, result);

    assertThat(result.get(SKU).get("computed").getRight()).isEqualTo(42);
  }

  @Test
  void apply_evaluatesBooleanExpression() {
    MappingContext ctx = ctx(SKU, List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(configWithCode("1 == 1", "BOOLEAN"), ctx, result);

    assertThat(result.get(SKU).get("computed").getRight()).isEqualTo(true);
  }

  // ---------------------------------------------------------------------------
  // apply() – with attribute placeholder replacement
  // ---------------------------------------------------------------------------

  @Test
  void apply_evaluatesExpressionWithReplacedAttribute() {
    Attribute attr = new Attribute();
    attr.setUkey("TITLE");
    attr.setReferences(Map.of("TEXT", "Bohrmaschine"));

    MappingContext ctx = ctx(SKU, List.of(attr));
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(configWithCode("$skuAttr(TITLE)$.getText()", "STRING"), ctx, result);

    assertThat(result.get(SKU).get("computed").getRight()).isEqualTo("Bohrmaschine");
  }

  @Test
  void apply_returnsEmptyStringWhenAttributeMissingInExpression() {
    MappingContext ctx = ctx(SKU, List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    // $skuAttr(MISSING)$.getText() → '' → SpEL evaluates '' to ""
    handler.apply(configWithCode("$skuAttr(MISSING)$.getText()", "STRING"), ctx, result);

    assertThat(result.get(SKU).get("computed").getRight()).isEqualTo("");
  }

  // ---------------------------------------------------------------------------
  // apply() – error handling
  // ---------------------------------------------------------------------------

  @Test
  void apply_silentlyIgnoresInvalidSpelExpression() {
    MappingContext ctx = ctx(SKU, List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    // Should not throw; invalid expressions are swallowed
    handler.apply(configWithCode("this is not valid spel ###", "STRING"), ctx, result);

    // No entry added for invalid expression
    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // apply() – result structure
  // ---------------------------------------------------------------------------

  @Test
  void apply_mergesIntoExistingSkuEntry() {
    MappingContext ctx = ctx(SKU, List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();
    result.put(SKU, new HashMap<>(Map.of("name", Pair.of("STRING", "Bohrmaschine"))));

    handler.apply(configWithCode("'added'", "STRING"), ctx, result);

    assertThat(result.get(SKU)).containsKeys("name", "computed");
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static MappingContext ctx(String sku, List<Attribute> attributes) {
    SkuAttributes skuAttributes = new SkuAttributes(Map.of(sku, attributes));
    Product product = new Product(new ProductMetaData("Test", 1L), List.of(), List.of(), Map.of(), List.of());
    return new MappingContext(skuAttributes, product, null, Map.of(), null, List.of());
  }

  private static MapConfig configWithCode(String javaCode, String targetFieldType) {
    MapConfig c = new MapConfig();
    c.setJavaCode(javaCode);
    c.setTargetField("computed");
    c.setTargetFieldType(targetFieldType);
    return c;
  }

  private static MapConfig configWithNullCode() {
    return configWithCode(null, "STRING");
  }
}
