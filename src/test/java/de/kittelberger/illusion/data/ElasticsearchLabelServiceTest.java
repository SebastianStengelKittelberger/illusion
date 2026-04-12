package de.kittelberger.illusion.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ElasticsearchLabelServiceTest {

  private MockRestServiceServer server;
  private ElasticsearchLabelService service;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    RestClient restClient = RestClient.builder(restTemplate)
      .baseUrl("http://localhost:9200")
      .build();
    service = new ElasticsearchLabelService(objectMapper, restClient);
  }

  // ---------------------------------------------------------------------------
  // loadFilterLabels
  // ---------------------------------------------------------------------------

  @Test
  void loadFilterLabels_returnsLabelsFromElasticsearch() throws Exception {
    String esResponse = labelsSearchResponse(Map.of("COLOR", "Farbe", "VOLTAGE", "Spannung"));
    server.expect(requestTo("http://localhost:9200/illusion-labels/_search"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess(esResponse, MediaType.APPLICATION_JSON));

    Map<String, String> result = service.loadFilterLabels("de", "de");

    assertThat(result)
      .containsEntry("COLOR", "Farbe")
      .containsEntry("VOLTAGE", "Spannung");

    server.verify();
  }

  @Test
  void loadFilterLabels_emptyHits_returnsEmptyMap() {
    server.expect(requestTo("http://localhost:9200/illusion-labels/_search"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess(emptySearchResponse(), MediaType.APPLICATION_JSON));

    Map<String, String> result = service.loadFilterLabels("de", "de");

    assertThat(result).isEmpty();
    server.verify();
  }

  @Test
  void loadFilterLabels_indexNotFound_returnsEmptyMap() {
    server.expect(requestTo("http://localhost:9200/illusion-labels/_search"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"error\":{\"type\":\"index_not_found_exception\",\"reason\":\"index_not_found\"}}"));

    assertThatNoException().isThrownBy(() -> service.loadFilterLabels("de", "de"));
    server.verify();
  }

  // ---------------------------------------------------------------------------
  // saveFilterLabels
  // ---------------------------------------------------------------------------

  @Test
  void saveFilterLabels_postsDocumentToElasticsearch() {
    server.expect(requestTo("http://localhost:9200/illusion-labels/_doc"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andRespond(withSuccess("{\"result\":\"created\"}", MediaType.APPLICATION_JSON));

    assertThatNoException().isThrownBy(() ->
      service.saveFilterLabels("de", "de", Map.of("COLOR", "Farbe"))
    );

    server.verify();
  }

  @Test
  void saveFilterLabels_emptyLabels_postsEmptyLabelsObject() {
    server.expect(requestTo("http://localhost:9200/illusion-labels/_doc"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andRespond(withSuccess("{\"result\":\"created\"}", MediaType.APPLICATION_JSON));

    assertThatNoException().isThrownBy(() ->
      service.saveFilterLabels("gb", "en", Map.of())
    );

    server.verify();
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private String labelsSearchResponse(Map<String, String> labels) throws Exception {
    String labelsJson = objectMapper.writeValueAsString(labels);
    return """
      {
        "hits": {
          "hits": [{
            "_source": {
              "namespace": "filter",
              "country": "de",
              "language": "de",
              "labels": %s
            }
          }]
        }
      }
      """.formatted(labelsJson);
  }

  private static String emptySearchResponse() {
    return "{\"hits\":{\"hits\":[]}}";
  }
}
