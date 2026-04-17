package de.kittelberger.illusion.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes mapped category documents to a dedicated Elasticsearch index
 * ({@code illusion-categories-{country}-{language}}).
 *
 * <p>Kept separate from {@link ElasticsearchIndexService} so that product and category
 * documents never share an index, and so the {@code ukey} identifier field (instead of
 * {@code sku}) is stored correctly on category documents.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchCategoryResultIndexService {

  private final ElasticsearchClient esClient;

  @Value("${elasticsearch.category-result-index-prefix:illusion-categories}")
  private String indexPrefix;

  @Value("${elasticsearch.max-fields:5000}")
  private String maxFields;

  public ElasticsearchCategoryResultIndexService(ElasticsearchClient esClient) {
    this.esClient = esClient;
  }

  /**
   * Bulk-indexes all mapped category documents.
   * The target index is {@code {prefix}-{country}-{language}} (all lowercase).
   * Each document stores a {@code ukey} field (not {@code sku}) as the human-readable identifier.
   *
   * @param results  map of categoryUkey → (fieldName → (fieldType, value))
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

    deleteIndex(indexName);
    createIndex(indexName);

    BulkRequest.Builder bulk = new BulkRequest.Builder();

    results.forEach((categoryUkey, fields) -> {
      Map<String, Object> document = new HashMap<>();
      fields.forEach((field, pair) -> document.put(field, pair.getRight()));
      document.put("ukey", categoryUkey);

      bulk.operations(op -> op
        .index(i -> i
          .index(indexName)
          .id(categoryUkey)
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
          "Bulk indexing had {} error(s) for category index '{}' — rolling back {} successful document(s)",
          failedIds.size(), indexName, succeededIds.size()
        );
        failedIds.forEach(id -> log.warn("  Failed category '{}': {}",
          id,
          response.items().stream()
            .filter(i -> id.equals(i.id()) && i.error() != null)
            .map(i -> i.error().reason())
            .findFirst().orElse("unknown reason")
        ));

        rollback(indexName, succeededIds);
      } else {
        log.info("Indexed {} category documents into Elasticsearch index '{}'", results.size(), indexName);
      }
    } catch (Exception e) {
      log.error("Failed to write {} category documents to Elasticsearch index '{}'",
        results.size(), indexName, e);
    }
  }

  private void createIndex(String indexName) {
    try {
      esClient.indices().create(r -> r
        .index(indexName)
        .settings(s -> s
          .mapping(m -> m.totalFields(tf -> tf.limit(maxFields)))
        )
      );
      log.info("Created Elasticsearch index '{}' with total_fields.limit={}", indexName, maxFields);
    } catch (Exception e) {
      log.warn("Could not create Elasticsearch index '{}' — will rely on auto-creation", indexName, e);
    }
  }

  private void deleteIndex(String indexName) {
    try {
      boolean exists = esClient.indices().exists(r -> r.index(indexName)).value();
      if (exists) {
        esClient.indices().delete(DeleteIndexRequest.of(r -> r.index(indexName)));
        log.info("Deleted existing Elasticsearch index '{}' before re-indexing", indexName);
      }
    } catch (Exception e) {
      log.warn("Could not delete Elasticsearch index '{}' before re-indexing — continuing anyway", indexName, e);
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
          "Rollback incomplete — {} category document(s) could not be deleted from index '{}': {}",
          rollbackFailures.size(), indexName, rollbackFailures
        );
      } else {
        log.info("Rollback successful — deleted {} category document(s) from index '{}'", ids.size(), indexName);
      }
    } catch (Exception e) {
      log.error("Rollback failed for category index '{}' — {} document(s) may remain: {}",
        indexName, ids.size(), ids, e);
    }
  }
}
