package de.kittelberger.illusion.model;

import de.kittelberger.webexport602w.solr.api.dto.AttrDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class SkuAttributes {

  private final Map<String, List<Pair<Attrval, Integer>>> attributesWithIndex;
  private final Map<String, List<Pair<Attrval, Integer>>> productAttributesWithIndex;

  public SkuAttributes(
    final List<Attrval> skuAttributes
  ) {
    this.attributesWithIndex = skuAttributes.stream().collect(
      Collectors.groupingBy(
        Attrval::getUkey,
        LinkedHashMap::new,
        Collectors.mapping(
          attrval -> Pair.of(attrval, skuAttributes.indexOf(attrval)),
          Collectors.toList()
        )
      )
    );
    this.skuAttributes = skuAttributes.stream().collect(
      Collectors.groupingBy(
        Attrval::getUkey,
        LinkedHashMap::new,
        Collectors.toList()
      )
    );
    this.productAttributesWithIndex = Collections.emptyMap();
    this.productAttributes = Collections.emptyMap();
  }

  public SkuAttributes(
    final List<Attrval> skuAttributes,
    final Map<Long, AttrDTO> preparedAttrDTOs
  ) {
    this.attributesWithIndex = skuAttributes.stream().collect(
      Collectors.groupingBy(
        Attrval::getUkey,
        LinkedHashMap::new,
        Collectors.mapping(
          attrval -> Pair.of(attrval, skuAttributes.indexOf(attrval)),
          Collectors.toList()
        )
      )
    );
    this.skuAttributes = skuAttributes.stream().collect(
      Collectors.groupingBy(
        Attrval::getUkey,
        LinkedHashMap::new,
        Collectors.toList()
      )
    );
    this.productAttributesWithIndex = Collections.emptyMap();
    this.productAttributes = Collections.emptyMap();
  }
  public SkuAttributes(
    final List<Attrval> skuAttributes,
    final List<Attrval> productAttributes,
    final Map<Long, AttrDTO> preparedAttrDTOs
  ) {
    this.attributesWithIndex = skuAttributes.stream().collect(
      Collectors.groupingBy(
        Attrval::getUkey,
        LinkedHashMap::new,
        Collectors.mapping(
          attrval -> Pair.of(attrval, skuAttributes.indexOf(attrval)),
          Collectors.toList()
        )
      )
    );
    this.skuAttributes = skuAttributes.stream().collect(
      Collectors.groupingBy(
        Attrval::getUkey,
        LinkedHashMap::new,
        Collectors.toList()
      )
    );
    this.productAttributesWithIndex = productAttributes.stream().collect(
      Collectors.groupingBy(
        Attrval::getUkey,
        LinkedHashMap::new,
        Collectors.mapping(
          attrval -> Pair.of(attrval, skuAttributes.indexOf(attrval)),
          Collectors.toList()
        )
      )
    );
    this.productAttributes = productAttributes.stream().collect(
      Collectors.groupingBy(
        Attrval::getUkey,
        LinkedHashMap::new,
        Collectors.toList()
      )
    );
  }

  private final Map<String, List<Attrval>> productAttributes;

  private final Map<String, List<Attrval>> skuAttributes;

  public Optional<List<Attrval>> getAttribute(String ukey) {
    return Optional.ofNullable(skuAttributes.get(ukey));
  }
  public Optional<List<Attrval>> getProductAttribute(String ukey) {
    return Optional.ofNullable(productAttributes.get(ukey));
  }

  public Optional<List<Attrval>> getAttributesInOrder(String... ukeys) {
    return getAttrvals(attributesWithIndex, ukeys);
  }

  public Optional<List<Attrval>> getProductAttributesInOrder(String... ukeys) {
    return getAttrvals(productAttributesWithIndex, ukeys);
  }

  private Optional<List<Attrval>> getAttrvals(Map<String, List<Pair<Attrval, Integer>>> productAttributesWithIndex, String[] ukeys) {
    List<Pair<Attrval, Integer>> result = new ArrayList<>();
    if(ukeys.length == 0){
      result.addAll(
        productAttributesWithIndex.values().stream()
          .flatMap(Collection::stream)
          .toList()
      );
    } else {
      for (String ukey : ukeys) {
        List<Pair<Attrval, Integer>> attrvals = productAttributesWithIndex.getOrDefault(ukey, null);
        if (attrvals != null) {
          result.addAll(attrvals);
        }
      }
    }
    return result.stream()
      .sorted(Comparator.comparingInt(Pair::getRight))
      .map(Pair::getLeft)
      .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        list -> list.isEmpty() ? Optional.empty() : Optional.of(list)
      ));
  }

  public Optional<Attrval> getFirstAttribute(String ukey) {return getAttribute(ukey).flatMap(list -> list.stream().findFirst());}

  public List<Attrval> getFirstAttributeValue(String... ukeys) {
    List<Attrval> result = new ArrayList<>();
    for (String ukey : ukeys) {
      Optional<List<Attrval>> attrvals = getAttribute(ukey);
      attrvals.ifPresent(result::addAll);
    }
    return result;
  }
}

