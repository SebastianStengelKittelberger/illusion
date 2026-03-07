package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.mapping.JavaCodeMappingHandler;
import de.kittelberger.illusion.mapping.MappingHandler;
import de.kittelberger.illusion.mapping.TextMappingHandler;
import de.kittelberger.illusion.model.DTOType;
import de.kittelberger.illusion.model.MapConfig;
import de.kittelberger.illusion.model.TargetType;
import de.kittelberger.webexport602w.solr.api.dto.AttrDTO;
import de.kittelberger.webexport602w.solr.api.dto.ProductDTO;
import de.kittelberger.webexport602w.solr.api.dto.SkuDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import de.kittelberger.webexport602w.solr.api.generated.ClngTextList2000;
import de.kittelberger.webexport602w.solr.api.generated.Product;
import de.kittelberger.webexport602w.solr.api.generated.Val;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

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
    indexingService = new IndexingService(loadDataService, handlers);
  }

  // ---------------------------------------------------------------------------
  // Shared MapConfig templates (matching the real configuration format)
  //
  //  Primary:  ukey=TITLE, dtoType=SKU   → first look at SKU attributes
  //  Fallback: ukey=$NAME$, dtoType=PRODUCT → fall back to product attribute
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
    when(loadDataService.getProductDTOs()).thenReturn(List.of());

    Map<String, Pair<String, Object>> result =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG, NAME_PRODUCT_FALLBACK), "DE", "de");

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Service interactions – always loaded
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_alwaysLoadsProducts() {
    when(loadDataService.getProductDTOs()).thenReturn(List.of());

    indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    verify(loadDataService).getProductDTOs();
  }

  @Test
  void indexProduct_withEmptyProductList_neverCallsGetSkuDTOsById() {
    when(loadDataService.getProductDTOs()).thenReturn(List.of());

    indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    verify(indexingService, never()).getSkuDTOsById(anyList());
  }

  @Test
  void indexProduct_withEmptyProductList_neverCallsGetAttrDTOs() {
    when(loadDataService.getProductDTOs()).thenReturn(List.of());

    indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    verify(loadDataService, never()).getAttrDTOs();
  }

  // ---------------------------------------------------------------------------
  // Per-product interactions
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_withOneProduct_callsGetSkuDTOsByIdWithSkuList() {
    ProductDTO product = productWithSkus("2608620766", "2608620767");
    when(loadDataService.getProductDTOs()).thenReturn(List.of(product));
    when(indexingService.getSkuDTOsById(anyList())).thenReturn(Map.of());
    when(loadDataService.getAttrDTOs()).thenReturn(List.of(attrDTO(8L, "TITLE")));

    indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    verify(indexingService).getSkuDTOsById(List.of("2608620766", "2608620767"));
  }

  @Test
  void indexProduct_withTwoProducts_callsGetSkuDTOsByIdTwice() {
    ProductDTO p1 = productWithSkus("2608620766");
    ProductDTO p2 = productWithSkus("2608620767", "2608620768");
    when(loadDataService.getProductDTOs()).thenReturn(List.of(p1, p2));
    when(indexingService.getSkuDTOsById(anyList())).thenReturn(Map.of());
    when(loadDataService.getAttrDTOs()).thenReturn(List.of());

    indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    verify(indexingService, times(2)).getSkuDTOsById(anyList());
    verify(indexingService).getSkuDTOsById(List.of("2608620766"));
    verify(indexingService).getSkuDTOsById(List.of("2608620767", "2608620768"));
  }

  @Test
  void indexProduct_withOneProduct_callsGetAttrDTOs() {
    ProductDTO product = productWithSkus("2608620766");
    when(loadDataService.getProductDTOs()).thenReturn(List.of(product));
    when(indexingService.getSkuDTOsById(anyList())).thenReturn(Map.of());
    when(loadDataService.getAttrDTOs()).thenReturn(List.of());

    indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    verify(loadDataService).getAttrDTOs();
  }

  // ---------------------------------------------------------------------------
  // MapConfig filtering
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_ignoresCategoryTargetConfigs() {
    MapConfig categoryConfig = mapConfig(TargetType.CATEGORY, DTOType.CATEGORY, "NAME", "TEXT", "name", "STRING", false);
    when(loadDataService.getProductDTOs()).thenReturn(List.of());

    assertThat(indexingService.indexProduct(List.of(categoryConfig), "DE", "de")).isEmpty();
  }

  @Test
  void indexProduct_withMixedTargets_onlyProcessesProductConfigs() {
    MapConfig categoryConfig = mapConfig(TargetType.CATEGORY, DTOType.CATEGORY, "NAME", "TEXT", "name", "STRING", false);
    ProductDTO product = productWithSkus("2608620766");
    when(loadDataService.getProductDTOs()).thenReturn(List.of(product));
    when(indexingService.getSkuDTOsById(anyList())).thenReturn(Map.of());
    when(loadDataService.getAttrDTOs()).thenReturn(List.of());

    // CATEGORY config is silently filtered; no exception
    assertThat(indexingService.indexProduct(List.of(TITLE_SKU_CONFIG, categoryConfig), "DE", "de")).isEmpty();
  }

  @Test
  void indexProduct_separatesFallbackFromPrimaryConfigs() {
    when(loadDataService.getProductDTOs()).thenReturn(List.of());

    // Both configs accepted; fallback ($NAME$/PRODUCT) computed separately from primary (TITLE/SKU)
    assertThat(indexingService.indexProduct(List.of(TITLE_SKU_CONFIG, NAME_PRODUCT_FALLBACK), "DE", "de")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Result map content – TEXT/STRING SKU mapping (TITLE_SKU_CONFIG)
  // ---------------------------------------------------------------------------

  @Test
  void indexProduct_withTitleSkuConfig_andMatchingAttrval_populatesResultMap() {
    // SKU has ukey=TITLE → matches TITLE_SKU_CONFIG → targetField="name"
    Attrval titleAttrval = attrval(8L, "TITLE", "PRO Screwdriver TX15 x 100 mm", null);

    SkuDTO sku = skuDTOWithAttrval(titleAttrval, "2608620766");
    ProductDTO product = productWithAttrval(titleAttrval, "2608620766");

    when(loadDataService.getProductDTOs()).thenReturn(List.of(product));
    when(indexingService.getSkuDTOsById(List.of("2608620766"))).thenReturn(Map.of("2608620766", List.of(sku)));
    when(loadDataService.getAttrDTOs()).thenReturn(List.of(attrDTO(8L, "TITLE")));

    Map<String, Pair<String, Object>> result =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    assertThat(result).containsKey("name");
    assertThat(result.get("name").getLeft()).isEqualTo("STRING");
    assertThat(result.get("name").getRight()).isEqualTo("PRO Screwdriver TX15 x 100 mm");
  }

  @Test
  void indexProduct_withLocalizedTitleAttrval_extractsCorrectLanguage() {
    ClngTextList2000 cltextval = clngText("de-DE", "Schraubendreher TX15 x 100 mm",
                                          "en-GB", "PRO Screwdriver TX15 x 100 mm");
    Attrval titleAttrval = attrval(8L, "TITLE", null, cltextval);

    SkuDTO sku = skuDTOWithAttrval(titleAttrval, "2608620766");
    ProductDTO product = productWithAttrval(titleAttrval, "2608620766");

    when(loadDataService.getProductDTOs()).thenReturn(List.of(product));
    when(indexingService.getSkuDTOsById(anyList())).thenReturn(Map.of("2608620766", List.of(sku)));
    when(loadDataService.getAttrDTOs()).thenReturn(List.of(attrDTO(8L, "TITLE")));

    Map<String, Pair<String, Object>> deResult =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");
    assertThat(deResult.get("name").getRight()).isEqualTo("Schraubendreher TX15 x 100 mm");

    Map<String, Pair<String, Object>> enResult =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "GB", "en");
    assertThat(enResult.get("name").getRight()).isEqualTo("PRO Screwdriver TX15 x 100 mm");
  }

  @Test
  void indexProduct_withTitleSkuConfig_targetFieldTypeIsString() {
    Attrval titleAttrval = attrval(8L, "TITLE", "Schraubendreher TX15 x 100 mm", null);
    SkuDTO sku = skuDTOWithAttrval(titleAttrval, "2608620766");
    ProductDTO product = productWithAttrval(titleAttrval, "2608620766");

    when(loadDataService.getProductDTOs()).thenReturn(List.of(product));
    when(indexingService.getSkuDTOsById(anyList())).thenReturn(Map.of("2608620766", List.of(sku)));
    when(loadDataService.getAttrDTOs()).thenReturn(List.of(attrDTO(8L, "TITLE")));

    Map<String, Pair<String, Object>> result =
      indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de");

    assertThat(result.get("name").getLeft()).isEqualTo("STRING");
  }

  @Test
  void indexProduct_withNonTextMappingType_doesNotPopulateResultMap() {
    MapConfig numberConfig = mapConfig(TargetType.PRODUCT, DTOType.SKU, "TITLE", "NUMBER", "title_d", "DOUBLE", false);
    Attrval titleAttrval = attrval(8L, "TITLE", "100", null);
    SkuDTO sku = skuDTOWithAttrval(titleAttrval, "2608620766");
    ProductDTO product = productWithAttrval(titleAttrval, "2608620766");

    when(loadDataService.getProductDTOs()).thenReturn(List.of(product));
    when(indexingService.getSkuDTOsById(anyList())).thenReturn(Map.of("2608620766", List.of(sku)));
    when(loadDataService.getAttrDTOs()).thenReturn(List.of(attrDTO(8L, "TITLE")));

    // mappingType=NUMBER is not processed by indexSku → resultMap stays empty
    assertThat(indexingService.indexProduct(List.of(numberConfig), "DE", "de")).isEmpty();
  }

  @Test
  void indexProduct_withMissingUkeyOnSku_doesNotPopulateResultMap() {
    // SKU has no TITLE attribute → getFirstAttribute("TITLE").isPresent() == false
    Attrval otherAttrval = attrval(99L, "COLOR", "red", null);
    SkuDTO sku = skuDTOWithAttrval(otherAttrval, "2608620766");
    ProductDTO product = productWithSkus("2608620766");

    when(loadDataService.getProductDTOs()).thenReturn(List.of(product));
    when(indexingService.getSkuDTOsById(anyList())).thenReturn(Map.of("2608620766", List.of(sku)));
    when(loadDataService.getAttrDTOs()).thenReturn(List.of(attrDTO(99L, "COLOR")));

    assertThat(indexingService.indexProduct(List.of(TITLE_SKU_CONFIG), "DE", "de")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static ProductDTO productWithSkus(String... skuCodes) {
    ProductDTO product = new ProductDTO();
    Product.Skus skus = new Product.Skus();
    for (String code : skuCodes) {
      Product.Skus.Sku sku = new Product.Skus.Sku();
      sku.setSku(code);
      skus.getSku().add(sku);
    }
    product.setSkus(skus);
    product.setAttrvals(new Val());
    return product;
  }

  private static ProductDTO productWithAttrval(Attrval attrval, String... skuCodes) {
    ProductDTO product = productWithSkus(skuCodes);
    product.getAttrvals().getAttrval().add(attrval);
    return product;
  }

  private static SkuDTO skuDTOWithAttrval(Attrval attrval, String skuCode) {
    SkuDTO sku = new SkuDTO();
    Val val = new Val();
    val.getAttrval().add(attrval);
    sku.setAttrvals(val);
    sku.setSku(skuCode);
    sku.setId(Long.parseLong(skuCode));
    return sku;
  }

  private static Attrval attrval(long attrdId, String ukey, String textval, ClngTextList2000 cltextval) {
    Attrval a = new Attrval();
    a.setAttrdId(BigInteger.valueOf(attrdId));
    a.setUkey(ukey);
    a.setTextval(textval);
    a.setCltextval(cltextval);
    return a;
  }

  private static ClngTextList2000 clngText(String cl1, String val1, String cl2, String val2) {
    ClngTextList2000 list = new ClngTextList2000();
    ClngTextList2000.ClText2000 e1 = new ClngTextList2000.ClText2000();
    e1.setCl(cl1); e1.setValue(val1);
    ClngTextList2000.ClText2000 e2 = new ClngTextList2000.ClText2000();
    e2.setCl(cl2); e2.setValue(val2);
    list.getClText2000().add(e1);
    list.getClText2000().add(e2);
    return list;
  }

  private static AttrDTO attrDTO(long id, String ukey) {
    AttrDTO attr = new AttrDTO();
    attr.setId(id);
    attr.setUkey(ukey);
    return attr;
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
}


