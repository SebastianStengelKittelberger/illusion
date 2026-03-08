package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.MediaObject;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.Reference;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class LoadDataService {

  private final RestClient restClient;

  public LoadDataService(RestClient boschAdapterRestClient) {
    this.restClient = boschAdapterRestClient;
  }

  @Cacheable("products")
  public List<Product> getProducts(String country, String language) {
    return restClient.get()
      .uri("/{country}/{language}/products", country, language)
      .retrieve()
      .body(new ParameterizedTypeReference<>() {});
  }

  @Cacheable("references")
  public List<Reference> getReferences(String country, String language) {
    return restClient.get()
      .uri("/{country}/{language}/references", country, language)
      .retrieve()
      .body(new ParameterizedTypeReference<>() {});
  }

  @Cacheable("mediaObjects")
  public List<MediaObject> getMediaObjects(String lang) {
    return restClient.get()
      .uri("/{lang}/media-objects", lang)
      .retrieve()
      .body(new ParameterizedTypeReference<>() {});
  }
}
