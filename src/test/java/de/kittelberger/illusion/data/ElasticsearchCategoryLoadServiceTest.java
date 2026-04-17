package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ElasticsearchCategoryLoadServiceTest {

  private MockRestServiceServer server;
  private ElasticsearchCategoryLoadService service;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    RestClient restClient = RestClient.builder(restTemplate)
      .baseUrl("http://localhost:9200")
      .build();
    service = new ElasticsearchCategoryLoadService(objectMapper, restClient);
    ReflectionTestUtils.setField(service, "indexPrefix", "bosch-categories");
  }

  // ---------------------------------------------------------------------------
  // loadCategories — success
  // ---------------------------------------------------------------------------

  @Test
  void loadCategories_returnsParsedCategories() {
    server.expect(requestTo("http://localhost:9200/bosch-categories-de-de/_search"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess(categorySearchResponse(), MediaType.APPLICATION_JSON));

    List<Category> result = service.loadCategories("de", "de");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().ukey()).isEqualTo("CAT-POWER-TOOLS");
    assertThat(result.getFirst().parentId()).isEqualTo(99L);
    assertThat(result.getFirst().skus()).containsExactly("SKU-001", "SKU-002");

    server.verify();
  }

  @Test
  void loadCategories_emptyHits_returnsEmptyList() {
    server.expect(requestTo("http://localhost:9200/bosch-categories-de-de/_search"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));

    assertThat(service.loadCategories("de", "de")).isEmpty();
    server.verify();
  }

  @Test
  void loadCategories_buildsIndexNameFromCountryAndLanguage() {
    server.expect(requestTo("http://localhost:9200/bosch-categories-gb-en/_search"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));

    service.loadCategories("gb", "en");

    server.verify();
  }

  // ---------------------------------------------------------------------------
  // loadCategories — index not found
  // ---------------------------------------------------------------------------

  @Test
  void loadCategories_indexNotFound_throwsIllegalStateException() {
    server.expect(requestTo("http://localhost:9200/bosch-categories-de-de/_search"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"error\":{\"type\":\"index_not_found_exception\",\"reason\":\"index_not_found\"}}"));

    assertThatThrownBy(() -> service.loadCategories("de", "de"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("bosch-categories-de-de");
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static String categorySearchResponse() {
    return """
      {
        "hits": {
          "hits": [{
            "_source": {
              "id": 42,
              "ukey": "CAT-POWER-TOOLS",
              "parentId": 99,
              "skus": ["SKU-001", "SKU-002"],
              "attributes": {}
            }
          }]
        }
      }
      """;
  }
}
