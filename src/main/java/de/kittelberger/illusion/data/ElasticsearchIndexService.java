package de.kittelberger.illusion.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes indexed SKU documents to Elasticsearch.
 * Only active when {@code elasticsearch.enabled=true} and an {@link ElasticsearchClient} bean is present.
 *
 * <p>All-or-nothing guarantee: if the Bulk API reports partial errors, all successfully written
 * documents from the same batch are deleted again so the index is never left in a partial state.
 */
@Slf4j
@Service
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchIndexService {

  private final ElasticsearchClient esClient;

  @Value("${elasticsearch.index-prefix:illusion}")
  private String indexPrefix;

  public ElasticsearchIndexService(ElasticsearchClient esClient) {
    this.esClient = esClient;
  }

  /**
   * Indexes all SKU documents into Elasticsearch via the Bulk API.
   * The target index is {@code {prefix}-{country}-{language}} (all lowercase).
   *
   * <p>On partial failure the successfully written documents are rolled back so the index
   * is never left in an inconsistent state. Errors are logged but never propagate —
   * the calling service always returns its HTTP result.
   *
   * @param results  map of skuKey → (fieldName → (fieldType, value))
   * @param country  two-letter country code (e.g. "DE")
   * @param language two-letter language code (e.g. "de")
   */
  public void indexResults(
    final Map<String, Map<String, Pair<String, Object>>> results,
    final String country,
    final String language
  ) {
    if (results.isEmpty()) {
      return;
    }

    String indexName = indexPrefix + "-" + country.toLowerCase() + "-" + language.toLowerCase();
    BulkRequest.Builder bulk = new BulkRequest.Builder();

    results.forEach((skuKey, fields) -> {
      Map<String, Object> document = new HashMap<>();
      fields.forEach((field, pair) -> document.put(field, pair.getRight()));
      document.put("sku", skuKey);

      bulk.operations(op -> op
        .index(i -> i
          .index(indexName)
          .id(skuKey)
          .document(document)
        )
      );
    });

    try {
      BulkResponse response = esClient.bulk(bulk.build());

      if (response.errors()) {
        List<String> failedIds = response.items().stream()
          .filter(item -> item.error() != null)
          .map(BulkResponseItem::id)
          .toList();
        List<String> succeededIds = response.items().stream()
          .filter(item -> item.error() == null)
          .map(BulkResponseItem::id)
          .toList();

        log.warn(
          "Elasticsearch bulk indexing had {} error(s) for index '{}' — rolling back {} successful document(s)",
          failedIds.size(), indexName, succeededIds.size()
        );
        failedIds.forEach(id -> log.warn("  Failed SKU '{}': {}",
          id,
          response.items().stream()
            .filter(i -> id.equals(i.id()) && i.error() != null)
            .map(i -> i.error().reason())
            .findFirst().orElse("unknown reason")
        ));

        rollback(indexName, succeededIds);
      } else {
        log.info("Indexed {} documents into Elasticsearch index '{}'", results.size(), indexName);
      }
    } catch (Exception e) {
      log.error("Failed to write {} documents to Elasticsearch index '{}' — result is still returned",
        results.size(), indexName, e);
    }
  }

  private void rollback(String indexName, List<String> ids) {
    if (ids.isEmpty()) {
      return;
    }
    BulkRequest.Builder deleteBulk = new BulkRequest.Builder();
    ids.forEach(id -> deleteBulk.operations(op -> op
      .delete(d -> d.index(indexName).id(id))
    ));
    try {
      BulkResponse deleteResponse = esClient.bulk(deleteBulk.build());
      if (deleteResponse.errors()) {
        List<String> rollbackFailures = deleteResponse.items().stream()
          .filter(item -> item.error() != null)
          .map(BulkResponseItem::id)
          .toList();
        log.error(
          "Rollback incomplete — {} document(s) could not be deleted from index '{}': {}",
          rollbackFailures.size(), indexName, rollbackFailures
        );
      } else {
        log.info("Rollback successful — deleted {} document(s) from index '{}'", ids.size(), indexName);
      }
    } catch (Exception e) {
      log.error("Rollback failed for index '{}' — {} document(s) may remain: {}",
        indexName, ids.size(), ids, e);
    }
  }
}
