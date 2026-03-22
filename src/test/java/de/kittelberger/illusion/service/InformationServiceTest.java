package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.Information;
import de.kittelberger.illusion.model.InformationRequestData;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.ProductMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformationServiceTest {

  @Mock
  private IndexingService indexingService;

  @Mock
  private LoadDataService loadDataService;

  @Mock
  private DataQualityService dataQualityService;

  private InformationService service;

  private static final String COUNTRY = "DE";
  private static final String LANGUAGE = "de";

  @BeforeEach
  void setUp() {
    service = new InformationService(indexingService, loadDataService, dataQualityService);
    when(indexingService.indexProduct(anyList(), anyString(), anyString())).thenReturn(Map.of());
  }

  // ---------------------------------------------------------------------------
  // Product and SKU UKEYs
  // ---------------------------------------------------------------------------

  @Test
  void loadInformation_collectsAllProductUkeys() {
    Product p = product(List.of(attr("TITLE"), attr("BRAND")), Map.of());
    when(loadDataService.getProducts(COUNTRY, LANGUAGE)).thenReturn(List.of(p));

    Information result = service.loadInformation(COUNTRY, LANGUAGE, requestData());

    assertThat(result.getProductUkeys()).containsExactlyInAnyOrder("TITLE", "BRAND");
  }

  @Test
  void loadInformation_collectsAllSkuUkeys() {
    Product p = product(List.of(), Map.of("SKU-001", List.of(attr("COLOR"), attr("SIZE"))));
    when(loadDataService.getProducts(COUNTRY, LANGUAGE)).thenReturn(List.of(p));

    Information result = service.loadInformation(COUNTRY, LANGUAGE, requestData());

    assertThat(result.getSkuUkeys()).containsExactlyInAnyOrder("COLOR", "SIZE");
  }

  // ---------------------------------------------------------------------------
  // Top-10 attribute selection by frequency
  // ---------------------------------------------------------------------------

  @Test
  void loadInformation_limitsProductAttributesToTop10() {
    // 11 distinct product attributes – result must be capped at 10
    List<Attribute> attrs = new ArrayList<>();
    for (int i = 1; i <= 11; i++) attrs.add(attr("ATTR_" + i));
    Product p = product(attrs, Map.of());
    when(loadDataService.getProducts(COUNTRY, LANGUAGE)).thenReturn(List.of(p));

    Information result = service.loadInformation(COUNTRY, LANGUAGE, requestData());

    assertThat(result.getProductAttributes()).hasSizeLessThanOrEqualTo(10);
  }

  @Test
  void loadInformation_limitsSkuAttributesToTop10() {
    List<Attribute> attrs = new ArrayList<>();
    for (int i = 1; i <= 11; i++) attrs.add(attr("ATTR_" + i));
    Product p = product(List.of(), Map.of("SKU-001", attrs));
    when(loadDataService.getProducts(COUNTRY, LANGUAGE)).thenReturn(List.of(p));

    Information result = service.loadInformation(COUNTRY, LANGUAGE, requestData());

    assertThat(result.getSkuAttributes()).hasSizeLessThanOrEqualTo(10);
  }

  @Test
  void loadInformation_returnsUniqueProductAttributesOnly() {
    // TITLE appears in two products – should only appear once in result
    Product p1 = product(List.of(attr("TITLE"), attr("BRAND")), Map.of());
    Product p2 = product(List.of(attr("TITLE"), attr("COLOR")), Map.of());
    when(loadDataService.getProducts(COUNTRY, LANGUAGE)).thenReturn(List.of(p1, p2));

    Information result = service.loadInformation(COUNTRY, LANGUAGE, requestData());

    long titleCount = result.getProductAttributes().stream()
      .filter(a -> "TITLE".equals(a.getUkey())).count();
    assertThat(titleCount).isEqualTo(1);
  }

  @Test
  void loadInformation_sortsProductAttributesByFrequencyDescending() {
    // COMMON appears in 3 products, RARE in 1
    Product p1 = product(List.of(attr("COMMON"), attr("RARE")), Map.of());
    Product p2 = product(List.of(attr("COMMON")), Map.of());
    Product p3 = product(List.of(attr("COMMON")), Map.of());
    when(loadDataService.getProducts(COUNTRY, LANGUAGE)).thenReturn(List.of(p1, p2, p3));

    Information result = service.loadInformation(COUNTRY, LANGUAGE, requestData());

    List<String> ukeys = result.getProductAttributes().stream()
      .map(Attribute::getUkey).toList();
    assertThat(ukeys.indexOf("COMMON")).isLessThan(ukeys.indexOf("RARE"));
  }

  // ---------------------------------------------------------------------------
  // Data quality
  // ---------------------------------------------------------------------------

  @Test
  void loadInformation_returnsDataQualityForSkuUkeys() {
    Product p = product(List.of(), Map.of("SKU-001", List.of(attr("COLOR"))));
    when(loadDataService.getProducts(COUNTRY, LANGUAGE)).thenReturn(List.of(p));

    de.kittelberger.illusion.model.DataQuality dq = de.kittelberger.illusion.model.DataQuality.builder()
      .ukey("COLOR").percentage("100%").skusWithoutUkey(List.of()).build();
    when(dataQualityService.getDataQuality(anyString(), anyList())).thenReturn(dq);

    Information result = service.loadInformation(COUNTRY, LANGUAGE, requestData());

    assertThat(result.getDataQualitySkus()).isNotEmpty();
  }

  @Test
  void loadInformation_limitsDataQualityEntriesTo10() {
    // 12 distinct SKU UKEYs
    List<Attribute> attrs = new ArrayList<>();
    for (int i = 1; i <= 12; i++) attrs.add(attr("ATTR_" + i));
    Product p = product(List.of(), Map.of("SKU-001", attrs));
    when(loadDataService.getProducts(COUNTRY, LANGUAGE)).thenReturn(List.of(p));

    de.kittelberger.illusion.model.DataQuality dq = de.kittelberger.illusion.model.DataQuality.builder()
      .ukey("ANY").percentage("50%").skusWithoutUkey(List.of()).build();
    when(dataQualityService.getDataQuality(anyString(), anyList())).thenReturn(dq);

    Information result = service.loadInformation(COUNTRY, LANGUAGE, requestData());

    assertThat(result.getDataQualitySkus()).hasSizeLessThanOrEqualTo(10);
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static Product product(List<Attribute> productAttrs, Map<String, List<Attribute>> skuAttrs) {
    return new Product(new ProductMetaData("Test", 1L), List.of(), productAttrs, skuAttrs, List.of());
  }

  private static Attribute attr(String ukey) {
    Attribute a = new Attribute();
    a.setUkey(ukey);
    return a;
  }

  private static InformationRequestData requestData() {
    InformationRequestData data = new InformationRequestData();
    data.setMapConfigs(List.of());
    return data;
  }
}
