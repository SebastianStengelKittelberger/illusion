package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.Image;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link LoadDataService} using a mocked REST server.
 */
class LoadDataServiceTest {

  private LoadDataService loadDataService;
  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.baseUrl("http://localhost:8080").build();
    loadDataService = new LoadDataService(restClient, new ObjectMapper());
  }

  // ---------------------------------------------------------------------------
  // Products
  // ---------------------------------------------------------------------------

  @Test
  void getProducts_deserializesProductList() {
    mockServer.expect(requestTo("http://localhost:8080/DE/de/products"))
      .andRespond(withSuccess(
        """
        [{"productMetaData":{"name":"Bohrmaschine","id":1,"artNo":"0601015200"},
          "skuMetaData":[],
          "productAttributes":[],
          "skuAttributes":{}}]
        """,
        MediaType.APPLICATION_JSON
      ));

    List<Product> products = loadDataService.getProducts("DE", "de");

    assertThat(products).hasSize(1);
    assertThat(products.getFirst().productMetaData().getName()).isEqualTo("Bohrmaschine");
    assertThat(products.getFirst().productMetaData().getId()).isEqualTo(1L);
    assertThat(products.getFirst().productMetaData().getArtNo()).isEqualTo("0601015200");
  }

  @Test
  void getProducts_returnsEmptyListOnEmptyResponse() {
    mockServer.expect(requestTo("http://localhost:8080/DE/de/products"))
      .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertThat(loadDataService.getProducts("DE", "de")).isEmpty();
  }

  @Test
  void getProducts_mapsSkuAttributes() {
    mockServer.expect(requestTo("http://localhost:8080/DE/de/products"))
      .andRespond(withSuccess(
        """
        [{"productMetaData":{"name":"Bohrmaschine","id":1,"artNo":"123"},
          "skuMetaData":[],
          "productAttributes":[],
          "skuAttributes":{
            "SKU-001":[
              {"ukey":"TITLE","referenceIds":{"attrId":8},
               "references":{"TEXT":"Bohrmaschine","BOOLEAN":false,"CLTEXT":"Bohrmaschine"}}
            ]
          }}]
        """,
        MediaType.APPLICATION_JSON
      ));

    List<Product> products = loadDataService.getProducts("DE", "de");

    Map<String, List<Attribute>> skuAttrs = products.getFirst().skuAttributes();
    assertThat(skuAttrs).hasSize(1);
    List<Attribute> attrs = skuAttrs.get("SKU-001");
    assertThat(attrs).hasSize(1);
    assertThat(attrs.getFirst().getUkey()).isEqualTo("TITLE");
    assertThat(attrs.getFirst().getReferences()).containsEntry("TEXT", "Bohrmaschine");
  }

  // ---------------------------------------------------------------------------
  // References
  // ---------------------------------------------------------------------------

  @Test
  void getReferences_deserializesReferenceList() {
    mockServer.expect(requestTo("http://localhost:8080/DE/de/references"))
      .andRespond(withSuccess(
        """
        [{"id":8,"ukey":"TITLE","attrClasses":{"left":"TITLE","right":[]}}]
        """,
        MediaType.APPLICATION_JSON
      ));

    List<Reference> refs = loadDataService.getReferences("DE", "de");

    assertThat(refs).hasSize(1);
    assertThat(refs.getFirst().getId()).isEqualTo(8L);
    assertThat(refs.getFirst().getUkey()).isEqualTo("TITLE");
  }

  // ---------------------------------------------------------------------------
  // Media objects
  // ---------------------------------------------------------------------------

  @Test
  void getMediaObjects_deserializesMediaObjectList() {
    mockServer.expect(requestTo("http://localhost:8080/de/de/media-objects"))
      .andRespond(withSuccess(
        """
        [{"name":"image1.jpg","attributes":[],"references":{},"mediaSpecifics":[]}]
        """,
        MediaType.APPLICATION_JSON
      ));

    Map<Long, Image> mediaObjects = loadDataService.getMediaObjects("de", "de");

    assertThat(mediaObjects).hasSize(1);
  }
}
