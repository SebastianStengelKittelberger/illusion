package de.kittelberger.illusion.service;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.SkuAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JavaParserServiceTest {

  private JavaParserService service;

  private static final String SKU = "SKU-001";

  @BeforeEach
  void setUp() {
    service = new JavaParserService();
  }

  // ---------------------------------------------------------------------------
  // No placeholders
  // ---------------------------------------------------------------------------

  @Test
  void replaceAttributeCalls_noPlaceholders_returnsCodeUnchanged() {
    SkuAttributes attrs = attrs(SKU, List.of());
    String code = "'hello world'";

    assertThat(service.replaceAttributeCalls(code, attrs, SKU)).isEqualTo("'hello world'");
  }

  // ---------------------------------------------------------------------------
  // getText()
  // ---------------------------------------------------------------------------

  @Test
  void replaceAttributeCalls_replacesGetTextCall() {
    Attribute attr = attr("TITLE", "Bohrmaschine", null, null);
    SkuAttributes attrs = attrs(SKU, List.of(attr));

    String result = service.replaceAttributeCalls("$skuAttr(TITLE)$.getText()", attrs, SKU);

    assertThat(result).isEqualTo("'Bohrmaschine'");
  }

  @Test
  void replaceAttributeCalls_replacesGetTextWithEmptyStringWhenAttributeMissing() {
    SkuAttributes attrs = attrs(SKU, List.of());

    String result = service.replaceAttributeCalls("$skuAttr(MISSING)$.getText()", attrs, SKU);

    assertThat(result).isEqualTo("''");
  }

  // ---------------------------------------------------------------------------
  // getNumVal()
  // ---------------------------------------------------------------------------

  @Test
  void replaceAttributeCalls_replacesGetNumValCall() {
    Attribute attr = attr("WEIGHT", null, null, "1.5");
    SkuAttributes attrs = attrs(SKU, List.of(attr));

    String result = service.replaceAttributeCalls("$skuAttr(WEIGHT)$.getNumVal()", attrs, SKU);

    assertThat(result).isEqualTo("'1.5'");
  }

  // ---------------------------------------------------------------------------
  // getLangSpecificText()
  // ---------------------------------------------------------------------------

  @Test
  void replaceAttributeCalls_replacesGetLangSpecificTextCall() {
    Attribute attr = attr("DESC", null, "Beschreibung", null);
    SkuAttributes attrs = attrs(SKU, List.of(attr));

    String result = service.replaceAttributeCalls("$skuAttr(DESC)$.getLangSpecificText()", attrs, SKU);

    assertThat(result).isEqualTo("'Beschreibung'");
  }

  @Test
  void replaceAttributeCalls_returnsEmptyStringForMissingCltextAttribute() {
    Attribute attr = attr("DESC", "English only", null, null);
    SkuAttributes attrs = attrs(SKU, List.of(attr));

    String result = service.replaceAttributeCalls("$skuAttr(DESC)$.getLangSpecificText()", attrs, SKU);

    assertThat(result).isEqualTo("''");
  }

  // ---------------------------------------------------------------------------
  // Unknown method
  // ---------------------------------------------------------------------------

  @Test
  void replaceAttributeCalls_replacesWithNullForUnknownMethodCall() {
    Attribute attr = attr("TITLE", "value", null, null);
    SkuAttributes attrs = attrs(SKU, List.of(attr));

    String result = service.replaceAttributeCalls("$skuAttr(TITLE)$.getUnknown()", attrs, SKU);

    assertThat(result).isEqualTo("null");
  }

  // ---------------------------------------------------------------------------
  // Multiple placeholders
  // ---------------------------------------------------------------------------

  @Test
  void replaceAttributeCalls_replacesMultiplePlaceholders() {
    Attribute titleAttr = attr("TITLE", "Bohrmaschine", null, null);
    Attribute colorAttr = attr("COLOR", "Blau", null, null);
    SkuAttributes attrs = attrs(SKU, List.of(titleAttr, colorAttr));

    String code = "$skuAttr(TITLE)$.getText() + ' - ' + $skuAttr(COLOR)$.getText()";
    String result = service.replaceAttributeCalls(code, attrs, SKU);

    assertThat(result).isEqualTo("'Bohrmaschine' + ' - ' + 'Blau'");
  }

  // ---------------------------------------------------------------------------
  // Single-quote escaping
  // ---------------------------------------------------------------------------

  @Test
  void replaceAttributeCalls_escapesSingleQuotesInValue() {
    Attribute attr = attr("NAME", "L'outil", null, null);
    SkuAttributes attrs = attrs(SKU, List.of(attr));

    String result = service.replaceAttributeCalls("$skuAttr(NAME)$.getText()", attrs, SKU);

    assertThat(result).isEqualTo("'L\\'outil'");
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static SkuAttributes attrs(String sku, List<Attribute> attributes) {
    return new SkuAttributes(Map.of(sku, attributes));
  }

  /** text → TEXT, cltext → CLTEXT, number → NUMBER reference values */
  private static Attribute attr(String ukey, String text, String cltext, String number) {
    Attribute a = new Attribute();
    a.setUkey(ukey);
    Map<String, Object> refs = new HashMap<>();
    if (text   != null) refs.put("TEXT",   text);
    if (cltext != null) refs.put("CLTEXT", cltext);
    if (number != null) refs.put("NUMBER", number);
    a.setReferences(refs.isEmpty() ? null : refs);
    return a;
  }
}
