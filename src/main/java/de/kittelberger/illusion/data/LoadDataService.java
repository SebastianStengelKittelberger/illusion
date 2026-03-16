package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.Image;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.Reference;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Service
public class LoadDataService {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public LoadDataService(RestClient boschAdapterRestClient, ObjectMapper objectMapper) {
    this.restClient = boschAdapterRestClient;
    this.objectMapper = objectMapper;
  }

  public List<Product> getProducts(String country, String language) {
    List<Product> products = new ArrayList<>();
    streamProducts(country, language, product -> {
      products.add(product);
      return true;
    });
    return products;
  }

  public void streamProducts(String country, String language, Predicate<Product> consumer) {
    restClient.get()
      .uri("{country}/{language}/products", country, language)
      .accept(MediaType.APPLICATION_JSON)
      .exchange((request, response) -> {
        if (response.getStatusCode().isError()) {
          throw new IllegalStateException("Adapter request failed with status " + response.getStatusCode());
        }

        try (InputStream body = response.getBody();
             var parser = objectMapper.createParser(body)) {
          if (parser.nextToken() == null) {
            return null;
          }
          if (parser.currentToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException("Expected products array from adapter");
          }

          while (parser.nextToken() != JsonToken.END_ARRAY) {
            Product product = objectMapper.readerFor(Product.class)
              .without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readValue(parser);
            if (!consumer.test(product)) {
              break;
            }
          }
          return null;
        } catch (IOException e) {
          throw new UncheckedIOException("Failed to stream products from adapter", e);
        }
      });
  }

  @Cacheable("references")
  public List<Reference> getReferences(String country, String language) {
    return restClient.get()
      .uri(country + "/" + language + "/references")
      .retrieve()
      .body(new ParameterizedTypeReference<>() {});
  }

  @Cacheable("mediaObjects")
  public Map<Long, Image> getMediaObjects(String country, String lang) {
    Map<Long, Image> resultMap = new HashMap<>();
    List<Image> result = restClient.get()
      .uri(country + "/" + lang + "/media-objects", lang)
      .retrieve()
      .body(new ParameterizedTypeReference<>() {});
    if (result == null) return resultMap;
    result.forEach(mediaObject -> resultMap.put(mediaObject.getMediaObjectId(), mediaObject));
    return resultMap;
  }

  @Cacheable("domain")
  public String getDomain(String country, String lang) {
    return restClient.get()
      .uri("/domain")
      .retrieve()
      .body(String.class);
  }
}
