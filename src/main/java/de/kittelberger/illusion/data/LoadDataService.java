package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.MediaObject;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.Reference;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Service
public class LoadDataService {

  private final RestClient restClient;

  public LoadDataService(RestClient boschAdapterRestClient) {
    this.restClient = boschAdapterRestClient;
  }

  @Cacheable("products")
  public List<Product> getProducts(String country, String language) {
    return restClient.get()
      .uri(country + "/" + language + "/products", country, language)
      .retrieve()
      .body(new ParameterizedTypeReference<>() {});
  }

  @Cacheable("references")
  public List<Reference> getReferences(String country, String language) {
    return restClient.get()
      .uri(country + "/" + language + "/references", country, language)
      .retrieve()
      .body(new ParameterizedTypeReference<>() {});
  }

  @Cacheable("mediaObjects")
  public Map<Long, MediaObject> getMediaObjects(String country, String lang) {
    Map<Long, MediaObject> resultMap = new HashMap<>();
    List<MediaObject> result = restClient.get()
      .uri(country + "/" + lang + "/media-objects", lang)
      .retrieve()
      .body(new ParameterizedTypeReference<>() {});
    if (result == null) return resultMap;
    result.forEach(mediaObject -> resultMap.put(mediaObject.getId(), mediaObject));
    return resultMap;
  }

  @Cacheable("domain")
  public String getDomain(String country, String lang) {
    return restClient.get()
      .uri(country + "/" + lang + "/domain")
      .retrieve()
      .body(String.class);
  }
}
