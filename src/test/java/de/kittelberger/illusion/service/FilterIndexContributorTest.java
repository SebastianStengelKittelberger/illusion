package de.kittelberger.illusion.service;

import de.kittelberger.illusion.mapping.MappingContext;
import de.kittelberger.illusion.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class FilterIndexContributorTest {

  private FilterIndexContributor contributor;

  @BeforeEach
  void setUp() {
    contributor = new FilterIndexContributor(new JavaParserService());
  }

  // ---------------------------------------------------------------------------
  // No-op cases
  // ---------------------------------------------------------------------------

  @Test
  void contribute_withNoFilterConfigs_doesNotModifyResults() {
    MapConfig plain = mapConfig("TITLE", "name", null);
    Map<String, Map<String, Pair<String, Object>>> results = mutableResults("SKU-001", "name", "Drill");

    contributor.contribute(List.of(plain), Map.of(), results);

    assertThat(results.get("SKU-001")).doesNotContainKey(FilterIndexContributor.FILTERS_FIELD);
  }

  @Test
  void contribute_withDisabledFilterConfig_doesNotAddFilters() {
    FilterConfig fc = filterConfig(false, FilterType.STANDARD, null);
    MapConfig config = mapConfig("COLOR", "color", fc);
    Map<String, Map<String, Pair<String, Object>>> results = mutableResults("SKU-001", "color", "red");

    contributor.contribute(List.of(config), Map.of(), results);

    assertThat(results.get("SKU-001")).doesNotContainKey(FilterIndexContributor.FILTERS_FIELD);
  }

  @Test
  void contribute_withEmptyResults_doesNotThrow() {
    FilterConfig fc = filterConfig(true, FilterType.STANDARD, null);
    MapConfig config = mapConfig("COLOR", "color", fc);

    assertThatNoException().isThrownBy(() ->
      contributor.contribute(List.of(config), Map.of(), new HashMap<>())
    );
  }

  // ---------------------------------------------------------------------------
  // STANDARD filter
  // ---------------------------------------------------------------------------

  @Test
  void contribute_standardFilter_copiesValueFromTargetField() {
    FilterConfig fc = filterConfig(true, FilterType.STANDARD, null);
    MapConfig config = mapConfig("COLOR", "color", fc);
    Map<String, Map<String, Pair<String, Object>>> results = mutableResults("SKU-001", "color", "red");

    contributor.contribute(List.of(config), Map.of(), results);

    Map<String, Object> filters = filtersOf(results, "SKU-001");
    assertThat(filters).containsEntry("COLOR", "red");
  }

  @Test
  void contribute_standardFilter_multipleUkeys_allCopied() {
    FilterConfig fc = filterConfig(true, FilterType.STANDARD, null);
    MapConfig colorConfig = mapConfig("COLOR", "color", fc);
    MapConfig voltageConfig = mapConfig("VOLTAGE", "voltage", fc);

    Map<String, Map<String, Pair<String, Object>>> results = new HashMap<>();
    Map<String, Pair<String, Object>> skuFields = new HashMap<>();
    skuFields.put("color", Pair.of("STRING", "red"));
    skuFields.put("voltage", Pair.of("STRING", "18V"));
    results.put("SKU-001", skuFields);

    contributor.contribute(List.of(colorConfig, voltageConfig), Map.of(), results);

    Map<String, Object> filters = filtersOf(results, "SKU-001");
    assertThat(filters)
      .containsEntry("COLOR", "red")
      .containsEntry("VOLTAGE", "18V");
  }

  @Test
  void contribute_standardFilter_whenTargetFieldMissing_doesNotAddEntry() {
    FilterConfig fc = filterConfig(true, FilterType.STANDARD, null);
    MapConfig config = mapConfig("COLOR", "color", fc);
    // "color" is not present in the results
    Map<String, Map<String, Pair<String, Object>>> results = mutableResults("SKU-001", "name", "Drill");

    contributor.contribute(List.of(config), Map.of(), results);

    // _filters map should not be created (or if created, should not have COLOR)
    Map<String, Pair<String, Object>> fields = results.get("SKU-001");
    if (fields.containsKey(FilterIndexContributor.FILTERS_FIELD)) {
      assertThat(filtersOf(results, "SKU-001")).doesNotContainKey("COLOR");
    }
  }

  @Test
  void contribute_standardFilter_multipleSKUs_eachGetsOwnFilters() {
    FilterConfig fc = filterConfig(true, FilterType.STANDARD, null);
    MapConfig config = mapConfig("COLOR", "color", fc);

    Map<String, Map<String, Pair<String, Object>>> results = new HashMap<>();
    results.put("SKU-001", mutableFields("color", "red"));
    results.put("SKU-002", mutableFields("color", "blue"));

    contributor.contribute(List.of(config), Map.of(), results);

    assertThat(filtersOf(results, "SKU-001")).containsEntry("COLOR", "red");
    assertThat(filtersOf(results, "SKU-002")).containsEntry("COLOR", "blue");
  }

  // ---------------------------------------------------------------------------
  // PREDICATE filter
  // ---------------------------------------------------------------------------

  @Test
  void contribute_predicateFilter_evaluatesTrueForMatchingValue() {
    FilterConfig fc = filterConfig(true, FilterType.PREDICATE, "$skuAttr(VOLTAGE)$.getText() == '18V'");
    MapConfig config = mapConfig("IS_18V", "voltage", fc);

    Map<String, Map<String, Pair<String, Object>>> results = mutableResults("SKU-001", "voltage", "18V");
    Map<String, MappingContext> contexts = Map.of("SKU-001", contextWithAttr("VOLTAGE", "18V"));

    contributor.contribute(List.of(config), contexts, results);

    assertThat(filtersOf(results, "SKU-001")).containsEntry("IS_18V", true);
  }

  @Test
  void contribute_predicateFilter_evaluatesFalseForNonMatchingValue() {
    FilterConfig fc = filterConfig(true, FilterType.PREDICATE, "$skuAttr(VOLTAGE)$.getText() == '18V'");
    MapConfig config = mapConfig("IS_18V", "voltage", fc);

    Map<String, Map<String, Pair<String, Object>>> results = mutableResults("SKU-001", "voltage", "36V");
    Map<String, MappingContext> contexts = Map.of("SKU-001", contextWithAttr("VOLTAGE", "36V"));

    contributor.contribute(List.of(config), contexts, results);

    assertThat(filtersOf(results, "SKU-001")).containsEntry("IS_18V", false);
  }

  @Test
  void contribute_predicateFilter_extractsStringValue() {
    FilterConfig fc = filterConfig(true, FilterType.PREDICATE, "$skuAttr(COLOR)$.getText()");
    MapConfig config = mapConfig("COLOR_VAL", "color", fc);

    Map<String, Map<String, Pair<String, Object>>> results = mutableResults("SKU-001", "color", "green");
    Map<String, MappingContext> contexts = Map.of("SKU-001", contextWithAttr("COLOR", "green"));

    contributor.contribute(List.of(config), contexts, results);

    assertThat(filtersOf(results, "SKU-001")).containsEntry("COLOR_VAL", "green");
  }

  @Test
  void contribute_predicateFilter_withNoContext_skipsEntry() {
    FilterConfig fc = filterConfig(true, FilterType.PREDICATE, "$skuAttr(VOLTAGE)$.getText() == '18V'");
    MapConfig config = mapConfig("IS_18V", "voltage", fc);

    Map<String, Map<String, Pair<String, Object>>> results = mutableResults("SKU-001", "voltage", "18V");
    // No context provided
    contributor.contribute(List.of(config), Map.of(), results);

    // _filters should not be written (no context → nothing evaluable)
    assertThat(results.get("SKU-001")).doesNotContainKey(FilterIndexContributor.FILTERS_FIELD);
  }

  @Test
  void contribute_predicateFilter_withInvalidExpression_doesNotThrow() {
    FilterConfig fc = filterConfig(true, FilterType.PREDICATE, "this is not valid spel !!!");
    MapConfig config = mapConfig("BAD", "name", fc);

    Map<String, Map<String, Pair<String, Object>>> results = mutableResults("SKU-001", "name", "Drill");
    Map<String, MappingContext> contexts = Map.of("SKU-001", contextWithAttr("NAME", "Drill"));

    assertThatNoException().isThrownBy(() ->
      contributor.contribute(List.of(config), contexts, results)
    );
  }

  // ---------------------------------------------------------------------------
  // Mixed STANDARD + PREDICATE
  // ---------------------------------------------------------------------------

  @Test
  void contribute_mixedFilterTypes_bothWrittenToSameFiltersMap() {
    FilterConfig stdFc = filterConfig(true, FilterType.STANDARD, null);
    FilterConfig predFc = filterConfig(true, FilterType.PREDICATE, "$skuAttr(VOLTAGE)$.getText() == '18V'");

    MapConfig colorConfig   = mapConfig("COLOR",   "color",   stdFc);
    MapConfig voltageConfig = mapConfig("IS_18V",  "voltage", predFc);

    Map<String, Map<String, Pair<String, Object>>> results = new HashMap<>();
    Map<String, Pair<String, Object>> fields = new HashMap<>();
    fields.put("color",   Pair.of("STRING", "red"));
    fields.put("voltage", Pair.of("STRING", "18V"));
    results.put("SKU-001", fields);

    Map<String, MappingContext> contexts = Map.of("SKU-001", contextWithAttr("VOLTAGE", "18V"));

    contributor.contribute(List.of(colorConfig, voltageConfig), contexts, results);

    Map<String, Object> filters = filtersOf(results, "SKU-001");
    assertThat(filters)
      .containsEntry("COLOR", "red")
      .containsEntry("IS_18V", true);
  }

  // ---------------------------------------------------------------------------
  // Test data helpers
  // ---------------------------------------------------------------------------

  private static FilterConfig filterConfig(boolean enabled, FilterType type, String predicate) {
    FilterConfig fc = new FilterConfig();
    fc.setEnabled(enabled);
    fc.setFilterType(type);
    fc.setPredicate(predicate);
    return fc;
  }

  private static MapConfig mapConfig(String ukey, String targetField, FilterConfig filterConfig) {
    MapConfig c = new MapConfig();
    c.setUkey(ukey);
    c.setTargetField(targetField);
    c.setTarget(TargetType.PRODUCT);
    c.setDtoType(DTOType.SKU);
    c.setMappingType("TEXT");
    c.setTargetFieldType("STRING");
    c.setFilterConfig(filterConfig);
    return c;
  }

  private static Map<String, Map<String, Pair<String, Object>>> mutableResults(
    String sku, String field, Object value
  ) {
    Map<String, Map<String, Pair<String, Object>>> results = new HashMap<>();
    results.put(sku, mutableFields(field, value));
    return results;
  }

  private static Map<String, Pair<String, Object>> mutableFields(String field, Object value) {
    Map<String, Pair<String, Object>> fields = new HashMap<>();
    fields.put(field, Pair.of("STRING", value));
    return fields;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> filtersOf(
    Map<String, Map<String, Pair<String, Object>>> results, String sku
  ) {
    Pair<String, Object> pair = results.get(sku).get(FilterIndexContributor.FILTERS_FIELD);
    assertThat(pair).as("_filters entry for %s", sku).isNotNull();
    return (Map<String, Object>) pair.getRight();
  }

  private static MappingContext contextWithAttr(String ukey, String textValue) {
    Attribute attr = new Attribute();
    attr.setUkey(ukey);
    attr.setReferenceIds(Map.of("attrId", 1L));
    attr.setReferences(Map.of("TEXT", textValue, "CLTEXT", textValue));

    SkuAttributes skuAttributes = new SkuAttributes(
      Map.of("SKU-001", List.of(attr)),
      List.of()
    );

    Product product = new Product(
      new ProductMetaData("Test Product", 1L, "ART-001"),
      List.of(), List.of(),
      Map.of("SKU-001", List.of(attr)),
      List.of()
    );

    return new MappingContext(skuAttributes, product, Locale.GERMANY, Map.of(), "https://test.example", List.of());
  }
}
