package de.kittelberger.illusion.data;

import de.kittelberger.webexport602w.solr.api.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LoadDataService}.
 * All tests use the XML fixtures in src/test/resources/xml-test/.
 */
@SpringBootTest
@TestPropertySource(properties = "xml.directory=${user.dir}/src/test/resources/xml-test")
class LoadDataServiceTest {

  @Autowired
  private LoadDataService loadDataService;

  // ---------------------------------------------------------------------------
  // ProductDTO
  // ---------------------------------------------------------------------------

  @Test
  void getProductDTOs_aggregatesMultipleFiles() {
    // test_001_product.xml (2 entries) + test_002_product.xml (1 entry) = 3
    List<ProductDTO> products = loadDataService.getProductDTOs();
    assertThat(products).hasSize(3);
  }

  @Test
  void getProductDTOs_mapsScalarFields() {
    ProductDTO product = loadDataService.getProductDTOs().stream()
        .filter(p -> Long.valueOf(100065563L).equals(p.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(product.getId()).isEqualTo(100065563L);
    assertThat(product.getMfact()).isEqualTo("Bosch");
    assertThat(product.getArtno()).isEqualTo("1 600 A03 9SA");
    assertThat(product.getAction()).isEqualTo('C');
    assertThat(product.getCdat()).isNotNull();
    assertThat(product.getUdat()).isNotNull();
  }

  @Test
  void getProductDTOs_mapsMultilingualName() {
    ProductDTO product = loadDataService.getProductDTOs().stream()
        .filter(p -> Long.valueOf(100065563L).equals(p.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(product.getName()).isNotNull();
    assertThat(product.getName()).containsEntry("de-DE", "Schraubendreher TX15 x 100 mm");
    assertThat(product.getName()).containsEntry("en-GB", "PRO Screwdriver TX15 x 100 mm");
  }

  @Test
  void getProductDTOs_mapsCategoryIds() {
    ProductDTO product = loadDataService.getProductDTOs().stream()
        .filter(p -> Long.valueOf(100065563L).equals(p.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(product.getCategories_category_id()).containsExactly(108174766L);
  }

  @Test
  void getProductDTOs_mapsMultipleCategoryIds() {
    ProductDTO product = loadDataService.getProductDTOs().stream()
        .filter(p -> Long.valueOf(100012345L).equals(p.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(product.getCategories_category_id()).containsExactlyInAnyOrder(108174766L, 108174767L);
  }

  @Test
  void getProductDTOs_mapsProducttypeIds() {
    ProductDTO product = loadDataService.getProductDTOs().stream()
        .filter(p -> Long.valueOf(100065563L).equals(p.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(product.getProducttypes_producttype_id()).containsExactly(100002348L);
  }

  // ---------------------------------------------------------------------------
  // SkuDTO
  // ---------------------------------------------------------------------------

  @Test
  void getSkuDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getSkuDTOs()).hasSize(2);
  }

  @Test
  void getSkuDTOs_mapsFields() {
    SkuDTO sku = loadDataService.getSkuDTOs().stream()
        .filter(s -> Long.valueOf(28842595L).equals(s.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(sku.getId()).isEqualTo(28842595L);
    assertThat(sku.getProduct()).isEqualTo(100065355L);
    assertThat(sku.getMfact()).isEqualTo("Bosch");
    assertThat(sku.getArtno()).isEqualTo("2 608 620 766");
    assertThat(sku.getCso()).isEqualTo("DE");
    assertThat(sku.getSku()).isEqualTo("2608620766");
    assertThat(sku.getGtin()).isEqualTo("06949509256577");
    assertThat(sku.getEan()).isEqualTo("6949509256577");
    assertThat(sku.getAction()).isEqualTo('C');
    assertThat(sku.getCdat()).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // AttrDTO
  // ---------------------------------------------------------------------------

  @Test
  void getAttrDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getAttrDTOs()).hasSize(2);
  }

  @Test
  void getAttrDTOs_mapsFields() {
    AttrDTO attr = loadDataService.getAttrDTOs().stream()
        .filter(a -> Long.valueOf(8L).equals(a.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(attr.getId()).isEqualTo(8L);
    assertThat(attr.getUkey()).isEqualTo("TITLE");
    assertThat(attr.getDatatype()).isEqualTo("text");
    assertThat(attr.getAction()).isEqualTo('C');
    assertThat(attr.getName()).containsEntry("de-DE", "Kategoriebezeichnung");
    assertThat(attr.getName()).containsEntry("en-GB", "Category designation");
    assertThat(attr.getShortname()).containsEntry("de-DE", "Kategoriebez.");
  }

  // ---------------------------------------------------------------------------
  // CategoryDTO
  // ---------------------------------------------------------------------------

  @Test
  void getCategoryDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getCategoryDTOs()).hasSize(2);
  }

  @Test
  void getCategoryDTOs_mapsFields() {
    CategoryDTO category = loadDataService.getCategoryDTOs().stream()
        .filter(c -> Long.valueOf(100101271L).equals(c.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(category.getId()).isEqualTo(100101271L);
    assertThat(category.getUkey()).isEqualTo("OCSBLAU");
    assertThat(category.getCtypeId()).isEqualTo(175L);
    assertThat(category.getCtypeUkey()).isEqualTo("OCS_BI_ROOT");
    assertThat(category.getAction()).isEqualTo('C');
    assertThat(category.getName()).containsEntry("de-DE", "Handwerk/Industrie (BI)");
    assertThat(category.getName()).containsEntry("en-GB", "Trade/industry (BI)");
    assertThat(category.getShortdesc()).containsEntry("de-DE", "Kurzbeschreibung DE");
  }

  @Test
  void getCategoryDTOs_mapsMediaobjectIds() {
    CategoryDTO category = loadDataService.getCategoryDTOs().stream()
        .filter(c -> Long.valueOf(100101272L).equals(c.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(category.getParentId()).isEqualTo(100101271L);
    assertThat(category.getMediaobjects_mediaobject_id()).containsExactly(200000704L);
  }

  // ---------------------------------------------------------------------------
  // CategorytypeDTO
  // ---------------------------------------------------------------------------

  @Test
  void getCategorytypeDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getCategorytypeDTOs()).hasSize(2);
  }

  @Test
  void getCategorytypeDTOs_mapsFields() {
    CategorytypeDTO ct = loadDataService.getCategorytypeDTOs().stream()
        .filter(c -> Long.valueOf(53L).equals(c.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(ct.getId()).isEqualTo(53L);
    assertThat(ct.getUkey()).isEqualTo("OCS2.0");
    assertThat(ct.getLevel()).isEqualTo(1L);
    assertThat(ct.getPos()).isEqualTo("1");
    assertThat(ct.getAction()).isEqualTo('C');
    assertThat(ct.getName()).containsEntry("de-DE", "OCS2.0");
    assertThat(ct.getShortdesc()).containsEntry("en-GB", "Template for categories of online catalogue");
  }

  // ---------------------------------------------------------------------------
  // LobtypeDTO
  // ---------------------------------------------------------------------------

  @Test
  void getLobtypeDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getLobtypeDTOs()).hasSize(2);
  }

  @Test
  void getLobtypeDTOs_mapsFields() {
    LobtypeDTO lobtype = loadDataService.getLobtypeDTOs().stream()
        .filter(l -> Long.valueOf(117L).equals(l.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(lobtype.getId()).isEqualTo(117L);
    assertThat(lobtype.getUkey()).isEqualTo("LORES_JPEG_RGB");
    assertThat(lobtype.getMediatype()).isEqualTo("image/jpeg");
    assertThat(lobtype.getMediaobjecttypeId()).isEqualTo(200000016L);
    assertThat(lobtype.getMediaobjecttypeUkey()).isEqualTo("IMAGES");
    assertThat(lobtype.getPos()).isEqualTo(6L);
    assertThat(lobtype.getAction()).isEqualTo('C');
    assertThat(lobtype.getName()).containsEntry("de-DE", "LORES_JPEG_RGB");
    assertThat(lobtype.getName()).containsEntry("en-GB", "LORES_JPEG_RGB");
  }

  // ---------------------------------------------------------------------------
  // MediaobjectDTO
  // ---------------------------------------------------------------------------

  @Test
  void getMediaobjectDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getMediaobjectDTOs()).hasSize(2);
  }

  @Test
  void getMediaobjectDTOs_mapsFields() {
    MediaobjectDTO mo = loadDataService.getMediaobjectDTOs().stream()
        .filter(m -> Long.valueOf(200000704L).equals(m.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(mo.getId()).isEqualTo(200000704L);
    assertThat(mo.getAction()).isEqualTo('C');
    assertThat(mo.getCdat()).isNotNull();
    assertThat(mo.getName()).containsEntry("de-DE", "2198f");
    assertThat(mo.getName()).containsEntry("en-GB", "2198f");
    assertThat(mo.getLobvalues()).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // MediaobjecttypeDTO
  // ---------------------------------------------------------------------------

  @Test
  void getMediaobjecttypeDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getMediaobjecttypeDTOs()).hasSize(2);
  }

  @Test
  void getMediaobjecttypeDTOs_mapsFields() {
    MediaobjecttypeDTO mt = loadDataService.getMediaobjecttypeDTOs().stream()
        .filter(m -> Long.valueOf(200000011L).equals(m.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(mt.getId()).isEqualTo(200000011L);
    assertThat(mt.getUkey()).isEqualTo("IMAGES");
    assertThat(mt.getLevel()).isEqualTo(1L);
    assertThat(mt.getPos()).isEqualTo("1");
    assertThat(mt.getParentId()).isNull();
    assertThat(mt.getAction()).isEqualTo('C');
    assertThat(mt.getName()).containsEntry("de-DE", "Bilder");
    assertThat(mt.getName()).containsEntry("en-GB", "Images");
  }

  @Test
  void getMediaobjecttypeDTOs_mapsParentId() {
    MediaobjecttypeDTO mt = loadDataService.getMediaobjecttypeDTOs().stream()
        .filter(m -> Long.valueOf(200000016L).equals(m.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(mt.getParentId()).isEqualTo(200000011L);
    assertThat(mt.getLevel()).isEqualTo(2L);
  }

  // ---------------------------------------------------------------------------
  // MfactDTO
  // ---------------------------------------------------------------------------

  @Test
  void getMfactDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getMfactDTOs()).hasSize(2);
  }

  @Test
  void getMfactDTOs_mapsFields() {
    MfactDTO mfact = loadDataService.getMfactDTOs().stream()
        .filter(m -> Long.valueOf(1L).equals(m.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(mfact.getId()).isEqualTo(1L);
    assertThat(mfact.getUkey()).isEqualTo("BOSCH");
    assertThat(mfact.getName()).isEqualTo("Bosch");
    assertThat(mfact.getAction()).isEqualTo('C');
    assertThat(mfact.getCdat()).isNotNull();
    assertThat(mfact.getUdat()).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // PriceDTO
  // ---------------------------------------------------------------------------

  @Test
  void getPriceDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getPriceDTOs()).hasSize(2);
  }

  @Test
  void getPriceDTOs_mapsFields() {
    PriceDTO price = loadDataService.getPriceDTOs().stream()
        .filter(p -> Long.valueOf(12156980L).equals(p.getSkuId()))
        .findFirst()
        .orElseThrow();

    assertThat(price.getSkuId()).isEqualTo(12156980L);
    assertThat(price.getPricetypeId()).isEqualTo(3L);
    assertThat(price.getCurrency()).isEqualTo("EUR");
    assertThat(price.getMfact()).isEqualTo("Bosch");
    assertThat(price.getArtno()).isEqualTo("0 601 015 200");
    assertThat(price.getCso()).isEqualTo("DE");
    assertThat(price.getSku()).isEqualTo("0601015200");
    assertThat(price.getPriceValue()).isEqualTo(207.0);
    assertThat(price.getAction()).isEqualTo('C');
    assertThat(price.getPricetypeName()).containsEntry("de-DE", "POM");
    assertThat(price.getPricetypeName()).containsEntry("en-GB", "Price w/o VAT");
  }

  // ---------------------------------------------------------------------------
  // ProducttypeDTO
  // ---------------------------------------------------------------------------

  @Test
  void getProducttypeDTOs_returnsTwoEntries() {
    assertThat(loadDataService.getProducttypeDTOs()).hasSize(2);
  }

  @Test
  void getProducttypeDTOs_mapsFields() {
    ProducttypeDTO pt = loadDataService.getProducttypeDTOs().stream()
        .filter(p -> Long.valueOf(100002348L).equals(p.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(pt.getId()).isEqualTo(100002348L);
    assertThat(pt.getUkey()).isEqualTo("DRILL");
    assertThat(pt.getLevel()).isEqualTo(2L);
    assertThat(pt.getParentId()).isEqualTo(100000001L);
    assertThat(pt.getPos()).isEqualTo("7");
    assertThat(pt.getAction()).isEqualTo('C');
    assertThat(pt.getName()).containsEntry("de-DE", "Bohrmaschine (elektrisch)");
    assertThat(pt.getName()).containsEntry("en-GB", "Drill (electric)");
    assertThat(pt.getShortdesc()).containsEntry("de-DE", "Bohrmaschine, elektrisch");
  }

  // ---------------------------------------------------------------------------
  // Edge cases
  // ---------------------------------------------------------------------------

  @Test
  void allMethods_returnNonEmptyLists() {
    assertThat(loadDataService.getProductDTOs()).isNotEmpty();
    assertThat(loadDataService.getSkuDTOs()).isNotEmpty();
    assertThat(loadDataService.getAttrDTOs()).isNotEmpty();
    assertThat(loadDataService.getCategoryDTOs()).isNotEmpty();
    assertThat(loadDataService.getCategorytypeDTOs()).isNotEmpty();
    assertThat(loadDataService.getLobtypeDTOs()).isNotEmpty();
    assertThat(loadDataService.getMediaobjectDTOs()).isNotEmpty();
    assertThat(loadDataService.getMediaobjecttypeDTOs()).isNotEmpty();
    assertThat(loadDataService.getMfactDTOs()).isNotEmpty();
    assertThat(loadDataService.getPriceDTOs()).isNotEmpty();
    assertThat(loadDataService.getProducttypeDTOs()).isNotEmpty();
  }

  @Test
  void allMethods_withInvalidDirectory_returnEmptyLists() {
    XmlFileLoader emptyLoader = new XmlFileLoader();
    emptyLoader.setXmlDirectory("/nonexistent/path/that/does/not/exist");
    LoadDataService emptyLoadDataService = new LoadDataService(emptyLoader);

    assertThat(emptyLoadDataService.getProductDTOs()).isEmpty();
    assertThat(emptyLoadDataService.getSkuDTOs()).isEmpty();
    assertThat(emptyLoadDataService.getMfactDTOs()).isEmpty();
  }
}
