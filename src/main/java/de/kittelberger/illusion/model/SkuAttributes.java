package de.kittelberger.illusion.model;

import java.util.*;
import java.util.stream.Collectors;

public class SkuAttributes {

  private final List<Attribute> skuAttributesList;
  private final List<Attribute> productAttributesList;
  private final Map<String, List<Attribute>> skuAttributesByUkey;
  private final Map<String, List<Attribute>> productAttributesByUkey;

  public SkuAttributes(final List<Attribute> skuAttributes) {
    this(skuAttributes, Collections.emptyList());
  }

  public SkuAttributes(
    final List<Attribute> skuAttributes,
    final List<Attribute> productAttributes
  ) {
    this.skuAttributesList = List.copyOf(skuAttributes);
    this.productAttributesList = List.copyOf(productAttributes);
    this.skuAttributesByUkey = groupByUkey(skuAttributes);
    this.productAttributesByUkey = groupByUkey(productAttributes);
  }

  private static Map<String, List<Attribute>> groupByUkey(List<Attribute> attrs) {
    return attrs.stream().collect(
      Collectors.groupingBy(Attribute::getUkey, LinkedHashMap::new, Collectors.toList())
    );
  }

  public Optional<List<Attribute>> getAttribute(String ukey) {
    return Optional.ofNullable(skuAttributesByUkey.get(ukey));
  }

  public Optional<List<Attribute>> getProductAttribute(String ukey) {
    return Optional.ofNullable(productAttributesByUkey.get(ukey));
  }

  public Optional<Attribute> getFirstAttribute(String ukey) {
    return getAttribute(ukey).flatMap(list -> list.stream().findFirst());
  }

  public List<Attribute> getFirstAttributeValue(String... ukeys) {
    List<Attribute> result = new ArrayList<>();
    for (String ukey : ukeys) {
      getAttribute(ukey).ifPresent(result::addAll);
    }
    return result;
  }

  /** Returns all matching attributes in their original list order. */
  public Optional<List<Attribute>> getAttributesInOrder(String... ukeys) {
    return filterInOrder(skuAttributesList, ukeys);
  }

  /** Returns all matching product attributes in their original list order. */
  public Optional<List<Attribute>> getProductAttributesInOrder(String... ukeys) {
    return filterInOrder(productAttributesList, ukeys);
  }

  private static Optional<List<Attribute>> filterInOrder(List<Attribute> source, String[] ukeys) {
    List<Attribute> result;
    if (ukeys.length == 0) {
      result = List.copyOf(source);
    } else {
      Set<String> ukeySet = Set.of(ukeys);
      result = source.stream()
        .filter(a -> ukeySet.contains(a.getUkey()))
        .toList();
    }
    return result.isEmpty() ? Optional.empty() : Optional.of(result);
  }
}

