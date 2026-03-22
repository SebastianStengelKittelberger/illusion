package de.kittelberger.illusion.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SkuAttributesTest {

  private static final String SKU = "SKU-001";

  // ---------------------------------------------------------------------------
  // getAttribute
  // ---------------------------------------------------------------------------

  @Test
  void getAttribute_whenUkeyExists_returnsList() {
    Attribute attr = attr("TITLE", "Bohrmaschine");
    SkuAttributes skuAttrs = new SkuAttributes(Map.of(SKU, List.of(attr)));

    Optional<List<Attribute>> result = skuAttrs.getAttribute(SKU, "TITLE");

    assertThat(result).isPresent();
    assertThat(result.get()).hasSize(1);
    assertThat(result.get().getFirst().getUkey()).isEqualTo("TITLE");
  }

  @Test
  void getAttribute_whenUkeyMissing_returnsEmpty() {
    SkuAttributes skuAttrs = new SkuAttributes(Map.of(SKU, List.of(attr("COLOR", "red"))));

    assertThat(skuAttrs.getAttribute(SKU, "TITLE")).isEmpty();
  }

  @Test
  void getAttribute_supportsMultipleAttributesForSameUkey() {
    Attribute a1 = attr("TAG", "tool");
    Attribute a2 = attr("TAG", "power");
    SkuAttributes skuAttrs = new SkuAttributes(Map.of(SKU, List.of(a1, a2)));

    assertThat(skuAttrs.getAttribute(SKU, "TAG")).hasValueSatisfying(list -> assertThat(list).hasSize(2));
  }

  // ---------------------------------------------------------------------------
  // getFirstAttribute
  // ---------------------------------------------------------------------------

  @Test
  void getFirstAttribute_returnsFirstOrEmptyWhenMissing() {
    Attribute first = attr("TITLE", "Erster");
    Attribute second = attr("TITLE", "Zweiter");
    SkuAttributes skuAttrs = new SkuAttributes(Map.of(SKU, List.of(first, second)));

    assertThat(skuAttrs.getFirstAttribute(SKU, "TITLE"))
      .hasValueSatisfying(a -> assertThat(a.getReferences().get("TEXT")).isEqualTo("Erster"));
    assertThat(skuAttrs.getFirstAttribute(SKU, "COLOR")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // getProductAttribute
  // ---------------------------------------------------------------------------

  @Test
  void getProductAttribute_returnsMappingOrEmptyWhenMissing() {
    Attribute productAttr = attr("BRAND", "Bosch");
    SkuAttributes skuAttrs = new SkuAttributes(Map.of(), List.of(productAttr));

    assertThat(skuAttrs.getProductAttribute("BRAND"))
      .hasValueSatisfying(list -> assertThat(list.getFirst().getUkey()).isEqualTo("BRAND"));
    assertThat(skuAttrs.getProductAttribute("MISSING")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // getFirstAttributeValue
  // ---------------------------------------------------------------------------

  @Test
  void getFirstAttributeValue_accumulatesMatchingUkeys() {
    Attribute title = attr("TITLE", "Schrauber");
    Attribute color = attr("COLOR", "red");
    SkuAttributes skuAttrs = new SkuAttributes(Map.of(SKU, List.of(title, color)));

    List<Attribute> result = skuAttrs.getFirstAttributeValue(SKU, "TITLE", "COLOR");

    assertThat(result).hasSize(2);
  }

  @Test
  void getFirstAttributeValue_ignoresMissingUkeys() {
    Attribute title = attr("TITLE", "Schrauber");
    SkuAttributes skuAttrs = new SkuAttributes(Map.of(SKU, List.of(title)));

    List<Attribute> result = skuAttrs.getFirstAttributeValue(SKU, "TITLE", "MISSING");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getUkey()).isEqualTo("TITLE");
  }

  // ---------------------------------------------------------------------------
  // getAttributesInOrder
  // ---------------------------------------------------------------------------

  @Test
  void getProductAttributesInOrder_filtersOrReturnsAllOrEmpty() {
    Attribute a = attr("TITLE", "x");
    Attribute b = attr("COLOR", "y");
    Attribute c = attr("WEIGHT", "z");
    SkuAttributes skuAttrs = new SkuAttributes(Map.of(), List.of(a, b, c));

    assertThat(skuAttrs.getProductAttributesInOrder())
      .hasValueSatisfying(list -> assertThat(list).hasSize(3));

    assertThat(skuAttrs.getProductAttributesInOrder("TITLE", "WEIGHT"))
      .hasValueSatisfying(list -> assertThat(list).extracting(Attribute::getUkey)
        .containsExactly("TITLE", "WEIGHT"));

    assertThat(skuAttrs.getProductAttributesInOrder("NONEXISTENT")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Test data
  // ---------------------------------------------------------------------------

  private static Attribute attr(String ukey, String text) {
    Attribute a = new Attribute();
    a.setUkey(ukey);
    a.setReferences(Map.of("TEXT", text));
    return a;
  }
}
