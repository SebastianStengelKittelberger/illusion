package de.kittelberger.illusion.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchIndexServiceTest {

  @Mock
  private ElasticsearchClient esClient;

  private ElasticsearchIndexService service;

  @BeforeEach
  void setUp() {
    service = new ElasticsearchIndexService(esClient);
    org.springframework.test.util.ReflectionTestUtils.setField(service, "indexPrefix", "illusion");
  }

  // ---------------------------------------------------------------------------
  // Empty results — no ES call at all
  // ---------------------------------------------------------------------------

  @Test
  void indexResults_withEmptyResults_doesNotCallElasticsearch() throws Exception {
    service.indexResults(Map.of(), "DE", "de");

    verifyNoMoreInteractions(esClient);
  }

  // ---------------------------------------------------------------------------
  // Successful bulk indexing
  // ---------------------------------------------------------------------------

  @Test
  void indexResults_successfulBulk_sendsSingleBulkRequestWithAllSkus() throws Exception {
    when(esClient.bulk(any(BulkRequest.class))).thenReturn(successResponse("SKU-001", "SKU-002"));

    service.indexResults(results("SKU-001", "SKU-002"), "DE", "de");

    ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(esClient, times(1)).bulk(captor.capture());
    assertThat(captor.getValue().operations()).hasSize(2);
  }

  @Test
  void indexResults_buildsIndexNameAsLowercasePrefixCountryLanguage() throws Exception {
    when(esClient.bulk(any(BulkRequest.class))).thenReturn(successResponse("SKU-001"));

    // Upper-case inputs must be normalized to lowercase
    service.indexResults(results("SKU-001"), "DE", "DE");

    ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(esClient).bulk(captor.capture());
    assertThat(captor.getValue().operations().getFirst().index().index()).isEqualTo("illusion-de-de");
  }

  // ---------------------------------------------------------------------------
  // Partial bulk failure → rollback
  // ---------------------------------------------------------------------------

  @Test
  void indexResults_onPartialFailure_rollbackDeletesOnlySuccessfulDocuments() throws Exception {
    BulkResponse partialFailure = partialFailureResponse(
      successItem("SKU-001"),
      failedItem("SKU-002", "mapper_parsing_exception")
    );
    when(esClient.bulk(any(BulkRequest.class)))
      .thenReturn(partialFailure)
      .thenReturn(successResponse());

    service.indexResults(results("SKU-001", "SKU-002"), "DE", "de");

    ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
    verify(esClient, times(2)).bulk(captor.capture());
    BulkRequest rollbackRequest = captor.getAllValues().get(1);
    assertThat(rollbackRequest.operations()).hasSize(1);
    assertThat(rollbackRequest.operations().getFirst().delete().id()).isEqualTo("SKU-001");
  }

  @Test
  void indexResults_whenAllDocumentsFail_noRollbackNeeded() throws Exception {
    BulkResponse allFailed = partialFailureResponse(
      failedItem("SKU-001", "strict_dynamic_mapping_exception"),
      failedItem("SKU-002", "strict_dynamic_mapping_exception")
    );
    when(esClient.bulk(any(BulkRequest.class))).thenReturn(allFailed);

    service.indexResults(results("SKU-001", "SKU-002"), "DE", "de");

    // Only the original bulk call — no rollback because nothing succeeded
    verify(esClient, times(1)).bulk(any(BulkRequest.class));
  }

  // ---------------------------------------------------------------------------
  // Complete ES failure — no rollback, no exception propagated
  // ---------------------------------------------------------------------------

  @Test
  void indexResults_whenEsThrowsException_doesNotPropagateAndNoRollback() throws Exception {
    when(esClient.bulk(any(BulkRequest.class))).thenThrow(new RuntimeException("ES connection refused"));

    assertThatNoException().isThrownBy(
      () -> service.indexResults(results("SKU-001"), "DE", "de")
    );
    // Only one call — no rollback attempt after a complete failure
    verify(esClient, times(1)).bulk(any(BulkRequest.class));
  }

  // ---------------------------------------------------------------------------
  // Test data builders
  // ---------------------------------------------------------------------------

  private static Map<String, Map<String, Pair<String, Object>>> results(String... skuKeys) {
    Map<String, Map<String, Pair<String, Object>>> map = new java.util.HashMap<>();
    for (String sku : skuKeys) {
      map.put(sku, Map.of("name", Pair.of("STRING", "Test Product")));
    }
    return map;
  }

  private static BulkResponse successResponse(String... ids) {
    List<BulkResponseItem> items = java.util.Arrays.stream(ids)
      .map(ElasticsearchIndexServiceTest::successItem)
      .toList();
    return BulkResponse.of(b -> b.errors(false).took(1L).items(items));
  }

  private static BulkResponse partialFailureResponse(BulkResponseItem... items) {
    return BulkResponse.of(b -> b.errors(true).took(1L).items(List.of(items)));
  }

  private static BulkResponseItem successItem(String id) {
    return BulkResponseItem.of(b -> b
      .operationType(OperationType.Index)
      .id(id)
      .index("illusion-de-de")
      .status(200)
    );
  }

  private static BulkResponseItem failedItem(String id, String reason) {
    return BulkResponseItem.of(b -> b
      .operationType(OperationType.Index)
      .id(id)
      .index("illusion-de-de")
      .status(400)
      .error(e -> e.reason(reason).type("es_error"))
    );
  }
}
