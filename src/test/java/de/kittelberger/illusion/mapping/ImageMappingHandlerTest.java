package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.Image;
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

class ImageMappingHandlerTest {

  private ImageMappingHandler handler;

  private static final String SKU = "SKU-001";
  private static final Long MEDIA_OBJECT_ID = 42L;

  @BeforeEach
  void setUp() {
    handler = new ImageMappingHandler();
  }

  // ---------------------------------------------------------------------------
  // supports()
  // ---------------------------------------------------------------------------

  @Test
  void supports_returnsTrueForIMAGEMappingType() {
    assertThat(handler.supports(config("IMAGE", "IMAGE"))).isTrue();
  }

  @Test
  void supports_returnsFalseForTEXTMappingType() {
    assertThat(handler.supports(config("TEXT", "STRING"))).isFalse();
  }

  @Test
  void supports_returnsFalseForCustomMappingType() {
    assertThat(handler.supports(config("CUSTOM", "IMAGE"))).isFalse();
  }

  // ---------------------------------------------------------------------------
  // apply() – successful image mapping
  // ---------------------------------------------------------------------------

  @Test
  void apply_mapsMediaObjectToResult() {
    Image image = Image.builder().fileName("tool.jpg").url("https://cdn.example.com/tool.jpg").build();
    MappingContext ctx = ctxWithImage("IMG_UKEY", MEDIA_OBJECT_ID, Map.of(MEDIA_OBJECT_ID, image));
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("IMAGE", "IMAGE", "IMG_UKEY", "image"), ctx, result);

    assertThat(result.get(SKU).get("image").getLeft()).isEqualTo("IMAGE");
    assertThat(result.get(SKU).get("image").getRight()).isEqualTo(image);
  }

  @Test
  void apply_mergesIntoExistingSkuEntry() {
    Image image = Image.builder().fileName("tool.jpg").build();
    MappingContext ctx = ctxWithImage("IMG_UKEY", MEDIA_OBJECT_ID, Map.of(MEDIA_OBJECT_ID, image));
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();
    result.put(SKU, new HashMap<>(Map.of("name", Pair.of("STRING", "Bohrmaschine"))));

    handler.apply(config("IMAGE", "IMAGE", "IMG_UKEY", "image"), ctx, result);

    assertThat(result.get(SKU)).containsKeys("name", "image");
  }

  // ---------------------------------------------------------------------------
  // apply() – skip conditions
  // ---------------------------------------------------------------------------

  @Test
  void apply_skipsWhenTargetFieldTypeIsNotIMAGE() {
    Image image = Image.builder().fileName("tool.jpg").build();
    MappingContext ctx = ctxWithImage("IMG_UKEY", MEDIA_OBJECT_ID, Map.of(MEDIA_OBJECT_ID, image));
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("IMAGE", "STRING", "IMG_UKEY", "image"), ctx, result);

    assertThat(result).isEmpty();
  }

  @Test
  void apply_skipsWhenAttributeHasNoReferenceIds() {
    Attribute attr = new Attribute();
    attr.setUkey("IMG_UKEY");
    attr.setReferenceIds(null);

    SkuAttributes skuAttributes = new SkuAttributes(Map.of(SKU, List.of(attr)));
    MappingContext ctx = new MappingContext(skuAttributes, dummyProduct(), null, Map.of(MEDIA_OBJECT_ID, Image.builder().build()), null, List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("IMAGE", "IMAGE", "IMG_UKEY", "image"), ctx, result);

    assertThat(result).isEmpty();
  }

  @Test
  void apply_skipsWhenReferenceIdsMissingMediaObjectId() {
    Attribute attr = new Attribute();
    attr.setUkey("IMG_UKEY");
    attr.setReferenceIds(Map.of("attrId", 99L));

    SkuAttributes skuAttributes = new SkuAttributes(Map.of(SKU, List.of(attr)));
    MappingContext ctx = new MappingContext(skuAttributes, dummyProduct(), null, Map.of(MEDIA_OBJECT_ID, Image.builder().build()), null, List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("IMAGE", "IMAGE", "IMG_UKEY", "image"), ctx, result);

    assertThat(result).isEmpty();
  }

  @Test
  void apply_skipsWhenMediaObjectIdNotFoundInMap() {
    MappingContext ctx = ctxWithImage("IMG_UKEY", MEDIA_OBJECT_ID, Map.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("IMAGE", "IMAGE", "IMG_UKEY", "image"), ctx, result);

    assertThat(result).isEmpty();
  }

  @Test
  void apply_skipsWhenAttributeNotFoundForUkey() {
    Attribute attr = new Attribute();
    attr.setUkey("OTHER_UKEY");
    SkuAttributes skuAttributes = new SkuAttributes(Map.of(SKU, List.of(attr)));
    MappingContext ctx = new MappingContext(skuAttributes, dummyProduct(), null, Map.of(), null, List.of());
    Map<String, Map<String, Pair<String, Object>>> result = new HashMap<>();

    handler.apply(config("IMAGE", "IMAGE", "IMG_UKEY", "image"), ctx, result);

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static MappingContext ctxWithImage(String ukey, Long mediaObjectId, Map<Long, Image> mediaMap) {
    Attribute attr = new Attribute();
    attr.setUkey(ukey);
    attr.setReferenceIds(Map.of("mediaObjectId", mediaObjectId));

    SkuAttributes skuAttributes = new SkuAttributes(Map.of(SKU, List.of(attr)));
    return new MappingContext(skuAttributes, dummyProduct(), null, mediaMap, null, List.of());
  }

  private static Product dummyProduct() {
    return new Product(new ProductMetaData("Test", 1L), List.of(), List.of(), Map.of(), List.of());
  }

  private static MapConfig config(String mappingType, String targetFieldType) {
    return config(mappingType, targetFieldType, "IMG_UKEY", "image");
  }

  private static MapConfig config(String mappingType, String targetFieldType, String ukey, String targetField) {
    MapConfig c = new MapConfig();
    c.setMappingType(mappingType);
    c.setTargetFieldType(targetFieldType);
    c.setUkey(ukey);
    c.setTargetField(targetField);
    return c;
  }
}
