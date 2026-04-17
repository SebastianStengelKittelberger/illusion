package de.kittelberger.illusion.data;

import de.kittelberger.illusion.model.Attribute;
import de.kittelberger.illusion.model.Image;
import de.kittelberger.illusion.model.Product;
import de.kittelberger.illusion.model.ProductMetaData;
import de.kittelberger.illusion.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LoadDataService} — all data is sourced from Elasticsearch.
 */
class LoadDataServiceTest {

  private ElasticsearchProductLoadService productLoadService;
  private ElasticsearchReferenceLoadService referenceLoadService;
  private ElasticsearchMediaObjectLoadService mediaObjectLoadService;
  private ElasticsearchConfigLoadService configLoadService;
  private LoadDataService loadDataService;

  @BeforeEach
  void setUp() {
    productLoadService = mock(ElasticsearchProductLoadService.class);
    referenceLoadService = mock(ElasticsearchReferenceLoadService.class);
    mediaObjectLoadService = mock(ElasticsearchMediaObjectLoadService.class);
    configLoadService = mock(ElasticsearchConfigLoadService.class);

    loadDataService = new LoadDataService(
      Optional.of(productLoadService),
      Optional.of(referenceLoadService),
      Optional.of(mediaObjectLoadService),
      Optional.of(configLoadService),
      Optional.empty()
    );
  }

  // ---------------------------------------------------------------------------
  // streamProducts / getProducts
  // ---------------------------------------------------------------------------

  @Test
  void streamProducts_delegatesToEsService() {
    doNothing().when(productLoadService).streamProducts(eq("DE"), eq("de"), any());

    loadDataService.streamProducts("DE", "de", p -> true);

    verify(productLoadService).streamProducts(eq("DE"), eq("de"), any());
  }

  @Test
  void getProducts_collectsAllProducts() {
    Product product = new Product(
      new ProductMetaData("Bohrmaschine", 1L, "0601015200"),
      List.of(), List.of(), Map.of(), List.of()
    );

    doAnswer(inv -> {
      @SuppressWarnings("unchecked")
      Predicate<Product> consumer = inv.getArgument(2);
      consumer.test(product);
      return null;
    }).when(productLoadService).streamProducts(eq("DE"), eq("de"), any());

    List<Product> result = loadDataService.getProducts("DE", "de");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().productMetaData().getName()).isEqualTo("Bohrmaschine");
  }

  @Test
  void streamProducts_throwsWhenEsNotEnabled() {
    LoadDataService noEs = new LoadDataService(
      Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
    );

    assertThatThrownBy(() -> noEs.streamProducts("DE", "de", p -> true))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("elasticsearch.enabled=true");
  }

  // ---------------------------------------------------------------------------
  // References
  // ---------------------------------------------------------------------------

  @Test
  void getReferences_delegatesToEsService() {
    Reference ref = mock(Reference.class);
    when(referenceLoadService.loadReferences("DE", "de")).thenReturn(List.of(ref));

    List<Reference> result = loadDataService.getReferences("DE", "de");

    assertThat(result).containsExactly(ref);
    verify(referenceLoadService).loadReferences("DE", "de");
  }

  @Test
  void getReferences_throwsWhenEsNotEnabled() {
    LoadDataService noEs = new LoadDataService(
      Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
    );

    assertThatThrownBy(() -> noEs.getReferences("DE", "de"))
      .isInstanceOf(IllegalStateException.class);
  }

  // ---------------------------------------------------------------------------
  // Media objects
  // ---------------------------------------------------------------------------

  @Test
  void getMediaObjects_indexedByMediaObjectId() {
    Image image = mock(Image.class);
    when(image.getMediaObjectId()).thenReturn(42L);
    when(mediaObjectLoadService.loadMediaObjects()).thenReturn(List.of(image));

    Map<Long, Image> result = loadDataService.getMediaObjects("DE", "de");

    assertThat(result).containsKey(42L);
    assertThat(result.get(42L)).isSameAs(image);
  }

  @Test
  void getMediaObjects_throwsWhenEsNotEnabled() {
    LoadDataService noEs = new LoadDataService(
      Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
    );

    assertThatThrownBy(() -> noEs.getMediaObjects("DE", "de"))
      .isInstanceOf(IllegalStateException.class);
  }

  // ---------------------------------------------------------------------------
  // Domain config
  // ---------------------------------------------------------------------------

  @Test
  void getDomain_delegatesToEsService() {
    when(configLoadService.loadDomain()).thenReturn("bosch.de");

    String result = loadDataService.getDomain("DE", "de");

    assertThat(result).isEqualTo("bosch.de");
    verify(configLoadService).loadDomain();
  }

  @Test
  void getDomain_throwsWhenEsNotEnabled() {
    LoadDataService noEs = new LoadDataService(
      Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
    );

    assertThatThrownBy(() -> noEs.getDomain("DE", "de"))
      .isInstanceOf(IllegalStateException.class);
  }

  // ---------------------------------------------------------------------------
  // Categories
  // ---------------------------------------------------------------------------

  @Test
  void getCategories_delegatesToEsService() {
    ElasticsearchCategoryLoadService categoryLoadService = mock(ElasticsearchCategoryLoadService.class);
    de.kittelberger.illusion.model.Category cat = mock(de.kittelberger.illusion.model.Category.class);
    when(categoryLoadService.loadCategories("DE", "de")).thenReturn(List.of(cat));

    LoadDataService svc = new LoadDataService(
      Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
      Optional.of(categoryLoadService)
    );

    List<de.kittelberger.illusion.model.Category> result = svc.getCategories("DE", "de");

    assertThat(result).containsExactly(cat);
    verify(categoryLoadService).loadCategories("DE", "de");
  }

  @Test
  void getCategories_throwsWhenEsNotEnabled() {
    LoadDataService noEs = new LoadDataService(
      Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
    );

    assertThatThrownBy(() -> noEs.getCategories("DE", "de"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("elasticsearch.enabled=true");
  }
}

