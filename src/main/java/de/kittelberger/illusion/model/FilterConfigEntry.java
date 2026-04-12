package de.kittelberger.illusion.model;

import lombok.Builder;
import lombok.Data;

/**
 * Read model for the filter-config endpoint.
 * Combines the filter configuration from a MapConfig entry with its display label.
 */
@Data
@Builder
public class FilterConfigEntry {

  private String ukey;
  private String targetField;
  private FilterConfig filterConfig;
  private String label;
}
