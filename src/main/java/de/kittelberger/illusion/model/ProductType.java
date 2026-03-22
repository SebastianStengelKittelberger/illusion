package de.kittelberger.illusion.model;

import java.util.List;

/**
 * Represents a product type resolved from the Bosch catalogue.
 * Product types form a hierarchy ({@code level}, {@code parentId}) and carry
 * a locale-specific {@code name} as resolved by the adapter.
 * {@code objAttrs} lists the attribute definitions assigned to this product type,
 * each with their associated {@link AttrClass} assignments.
 */
public record ProductType(
  Long id,
  String ukey,
  String name,
  Long parentId,
  Long level,
  List<ObjAttr> objAttrs
) {}
