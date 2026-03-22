package de.kittelberger.illusion.service;

import de.kittelberger.illusion.data.LoadDataService;
import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.DataQuality;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.ProductMetaData;
import de.kittelberger.illusion.model.SkuMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataQualityServiceTest {

  @Mock
  private LoadDataService loadDataService;

  private DataQualityService service;

  private static final String COUNTRY = "DE";
  private static final String LANGUAGE = "de";
  private static final String UKEY = "TITLE";

  @BeforeEach
  void setUp() {
    service = new DataQualityService(loadDataService);
  }

  // ---------------------------------------------------------------------------
  // Full coverage
  // ---------------------------------------------------------------------------

  @Test
  void getDataQuality_allSkusHaveUkey_returns100Percent() {
    Product product = product(
      List.of(skuMeta("SKU-001"), skuMeta("SKU-002")),
      Map.of(
        "SKU-001", List.of(attr(UKEY)),
        "SKU-002", List.of(attr(UKEY))
      )
    );

    DataQuality result = service.getDataQuality(UKEY, List.of(product));

    assertThat(result.getPercentage()).startsWith("100%");
    assertThat(result.getSkusWithoutUkey()).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Zero coverage
  // ---------------------------------------------------------------------------

  @Test
  void getDataQuality_noSkuHasUkey_returns0Percent() {
    Product product = product(
      List.of(skuMeta("SKU-001"), skuMeta("SKU-002")),
      Map.of(
        "SKU-001", List.of(attr("COLOR")),
        "SKU-002", List.of(attr("COLOR"))
      )
    );

    DataQuality result = service.getDataQuality(UKEY, List.of(product));

    assertThat(result.getPercentage()).startsWith("0%");
    assertThat(result.getSkusWithoutUkey()).containsExactlyInAnyOrder("SKU-001", "SKU-002");
  }

  // ---------------------------------------------------------------------------
  // Partial coverage
  // ---------------------------------------------------------------------------

  @Test
  void getDataQuality_partialCoverage_returnsPercentageMissingListAndMessage() {
    Product product = product(
      List.of(skuMeta("SKU-001"), skuMeta("SKU-002"), skuMeta("SKU-003"), skuMeta("SKU-004")),
      Map.of(
        "SKU-001", List.of(attr(UKEY)),
        "SKU-002", List.of(attr(UKEY)),
        "SKU-003", List.of(attr("COLOR")),
        "SKU-004", List.of(attr("COLOR"))
      )
    );

    DataQuality result = service.getDataQuality(UKEY, List.of(product));

    assertThat(result.getPercentage()).startsWith("50%");
    assertThat(result.getPercentage()).contains("von 4 Skus.");
    assertThat(result.getSkusWithoutUkey()).containsExactlyInAnyOrder("SKU-003", "SKU-004");
  }

  // ---------------------------------------------------------------------------
  // Multiple products
  // ---------------------------------------------------------------------------

  @Test
  void getDataQuality_aggregatesAcrossMultipleProducts() {
    Product p1 = product(List.of(skuMeta("SKU-A")), Map.of("SKU-A", List.of(attr(UKEY))));
    Product p2 = product(List.of(skuMeta("SKU-B")), Map.of("SKU-B", List.of(attr("COLOR"))));

    DataQuality result = service.getDataQuality(UKEY, List.of(p1, p2));

    assertThat(result.getPercentage()).startsWith("50%");
    assertThat(result.getSkusWithoutUkey()).containsExactly("SKU-B");
  }

  // ---------------------------------------------------------------------------
  // Delegation to LoadDataService
  // ---------------------------------------------------------------------------

  @Test
  void getDataQuality_delegatesToLoadDataService() {
    Product product = product(List.of(skuMeta("SKU-001")), Map.of("SKU-001", List.of(attr(UKEY))));
    when(loadDataService.getProducts(COUNTRY, LANGUAGE)).thenReturn(List.of(product));

    DataQuality result = service.getDataQuality(UKEY, COUNTRY, LANGUAGE);

    assertThat(result.getUkey()).isEqualTo(UKEY);
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static Product product(List<SkuMetaData> skuMetas, Map<String, List<Attribute>> skuAttributes) {
    return new Product(new ProductMetaData("Test", 1L), skuMetas, List.of(), skuAttributes, List.of());
  }

  private static SkuMetaData skuMeta(String sku) {
    SkuMetaData meta = new SkuMetaData(sku, null);
    meta.setSku(sku);
    return meta;
  }

  private static Attribute attr(String ukey) {
    Attribute a = new Attribute();
    a.setUkey(ukey);
    return a;
  }
}
