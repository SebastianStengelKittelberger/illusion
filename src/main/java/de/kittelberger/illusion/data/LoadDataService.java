package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.Category;
import de.kittelberger.illusion.model.Image;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.Reference;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class LoadDataService {

  private final Optional<ElasticsearchProductLoadService> elasticsearchProductLoadService;
  private final Optional<ElasticsearchReferenceLoadService> elasticsearchReferenceLoadService;
  private final Optional<ElasticsearchMediaObjectLoadService> elasticsearchMediaObjectLoadService;
  private final Optional<ElasticsearchConfigLoadService> elasticsearchConfigLoadService;
  private final Optional<ElasticsearchCategoryLoadService> elasticsearchCategoryLoadService;

  public LoadDataService(
    Optional<ElasticsearchProductLoadService> elasticsearchProductLoadService,
    Optional<ElasticsearchReferenceLoadService> elasticsearchReferenceLoadService,
    Optional<ElasticsearchMediaObjectLoadService> elasticsearchMediaObjectLoadService,
    Optional<ElasticsearchConfigLoadService> elasticsearchConfigLoadService,
    Optional<ElasticsearchCategoryLoadService> elasticsearchCategoryLoadService
  ) {
    this.elasticsearchProductLoadService = elasticsearchProductLoadService;
    this.elasticsearchReferenceLoadService = elasticsearchReferenceLoadService;
    this.elasticsearchMediaObjectLoadService = elasticsearchMediaObjectLoadService;
    this.elasticsearchConfigLoadService = elasticsearchConfigLoadService;
    this.elasticsearchCategoryLoadService = elasticsearchCategoryLoadService;
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
    elasticsearchProductLoadService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .streamProducts(country, language, consumer);
  }

  @Cacheable("references")
  public List<Reference> getReferences(String country, String language) {
    return elasticsearchReferenceLoadService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .loadReferences(country, language);
  }

  @Cacheable("mediaObjects")
  public Map<Long, Image> getMediaObjects(String country, String lang) {
    List<Image> images = elasticsearchMediaObjectLoadService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .loadMediaObjects();
    Map<Long, Image> resultMap = new HashMap<>();
    images.forEach(image -> resultMap.put(image.getMediaObjectId(), image));
    return resultMap;
  }

  @Cacheable("domain")
  public String getDomain(String country, String lang) {
    return elasticsearchConfigLoadService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .loadDomain();
  }

  public List<Category> getCategories(String country, String language) {
    return elasticsearchCategoryLoadService
      .orElseThrow(() -> new IllegalStateException("Elasticsearch is not enabled — set elasticsearch.enabled=true"))
      .loadCategories(country, language);
  }
}
