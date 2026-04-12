package de.kittelberger.illusion.model;

import lombok.Data;

@Data
public class FilterConfig {

  /** Whether this ukey should be included as a filter. */
  private boolean enabled;

  /**
   * STANDARD: filter by the exact value of the mapped targetField.
   * PREDICATE: evaluate a custom SpEL expression using $skuAttr(UKEY)$.getText() tokens.
   */
  private FilterType filterType = FilterType.STANDARD;

  /**
   * Only used when filterType is PREDICATE.
   * SpEL expression that resolves to a Boolean or String, e.g.:
   * {@code $skuAttr(VOLTAGE)$.getText() == '18V'}
   */
  private String predicate;

  /** Display order in the filter panel (lower = first). */
  private Integer order;

  /** Optional grouping label, e.g. "Technik", "Ausstattung". */
  private String group;
}
