package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.Category;
import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.model.TargetType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTextMappingHandlerTest {

  private CategoryTextMappingHandler handler;

  private static final String CATEGORY_UKEY = "CAT-001";

  @BeforeEach
  void setUp() {
    handler = new CategoryTextMappingHandler();
  }

  // ---------------------------------------------------------------------------
  // supports()
  // ---------------------------------------------------------------------------

  @Test
  void supports_returnsTrueForCategoryTargetAndTEXTMappingAndSTRINGType() {
    assertThat(handler.supports(config("TITLE", "TEXT", "STRING"))).isTrue();
  }

  @Test
  void supports_returnsFalseForProductTarget() {
    MapConfig c = config("TITLE", "TEXT", "STRING");
    c.setTarget(TargetType.PRODUCT);
    assertThat(handler.supports(c)).isFalse();
  }

  @Test
  void supports_returnsFalseForNonTEXTMappingType() {
    assertThat(handler.supports(config("TITLE", "IMAGE", "STRING"))).isFalse();
  }

  @Test
  void supports_returnsFalseForNonSTRINGTargetFieldType() {
    assertThat(handler.supports(config("TITLE", "TEXT", "IMAGE"))).isFalse();
  }

  // ---------------------------------------------------------------------------
  // apply() — attribute extraction
  // ---------------------------------------------------------------------------

  @Test
  void apply_extractsTextValue() {
    Category cat = category(CATEGORY_UKEY, attr("TITLE", "Power Tools", null));
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("TITLE", "TEXT", "STRING"), new CategoryMappingContext(cat, null), result);

    assertThat(result.get(CATEGORY_UKEY).get("name").getRight()).isEqualTo("Power Tools");
  }

  @Test
  void apply_prefersCltextOverText() {
    Category cat = category(CATEGORY_UKEY, attr("TITLE", "Power Tools EN", "Elektrowerkzeuge"));
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("TITLE", "TEXT", "STRING"), new CategoryMappingContext(cat, null), result);

    assertThat(result.get(CATEGORY_UKEY).get("name").getRight()).isEqualTo("Elektrowerkzeuge");
  }

  @Test
  void apply_writesNullWhenUkeyNotFound() {
    Category cat = category(CATEGORY_UKEY, attr("OTHER_ATTR", "something", null));
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("TITLE", "TEXT", "STRING"), new CategoryMappingContext(cat, null), result);

    assertThat(result.get(CATEGORY_UKEY).get("name").getRight()).isNull();
  }

  @Test
  void apply_mergesIntoExistingEntry() {
    Category cat = category(CATEGORY_UKEY, attr("TITLE", "Power Tools", null));
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();
    result.put(CATEGORY_UKEY, new HashMap<>(Map.of("skus", Pair.of("LIST", List.of("SKU-1")))));

    handler.apply(config("TITLE", "TEXT", "STRING"), new CategoryMappingContext(cat, null), result);

    assertThat(result.get(CATEGORY_UKEY)).containsKeys("skus", "name");
    assertThat(result.get(CATEGORY_UKEY).get("name").getRight()).isEqualTo("Power Tools");
  }

  @Test
  void apply_keysResultByCategoryUkey() {
    Category cat = category(CATEGORY_UKEY, attr("TITLE", "Power Tools", null));
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("TITLE", "TEXT", "STRING"), new CategoryMappingContext(cat, null), result);

    assertThat(result).containsKey(CATEGORY_UKEY);
    assertThat(result).doesNotContainKey("other-key");
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static Category category(String ukey, Attribute attribute) {
    Map<String, List<Attribute>> attrs = new HashMap<>();
    attrs.put(attribute.getUkey(), List.of(attribute));
    return new Category(1L, ukey, null, List.of(), attrs);
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

  private static MapConfig config(String ukey, String mappingType, String targetFieldType) {
    MapConfig c = new MapConfig();
    c.setTarget(TargetType.CATEGORY);
    c.setMappingType(mappingType);
    c.setTargetFieldType(targetFieldType);
    c.setUkey(ukey);
    c.setTargetField("name");
    return c;
  }
}
