package de.kittelberger.illusion.model;

public enum FilterType {
  /** Filter by exact value of the mapped targetField. */
  STANDARD,
  /** Filter by evaluating a custom SpEL predicate using $skuAttr(UKEY)$.getText() tokens. */
  PREDICATE
}
