package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static de.kittelberger.illusion.util.AttributeUtil.extractText;

@Component
public class ComplexMappingHandler implements MappingHandler{

  @Override
  public boolean supports(MapConfig config) {
    return config.getComplexMapping() != null && !CollectionUtils.isEmpty(config.getComplexMapping().getReferencedAttrClasses());
  }

  @Override
  public void apply(
    final MapConfig config,
    final MappingContext ctx,
    Map<String, Map<String, Pair<String, Object>>> result
  ) {
    SkuAttributes skuAttributes = ctx.skuAttributes();
    List<Reference> referenceList = new ArrayList<>(ctx.referenceList());
    referenceList = referenceList
      .stream()
      .filter(reference -> reference.getAttrClasses() != null && reference.getAttrClasses().right() != null && reference.getAttrClasses().right()
        .stream()
        .map(AttrClass::getUkey)
        .anyMatch(ukey -> config.getComplexMapping().getReferencedAttrClasses() != null && config.getComplexMapping().getReferencedAttrClasses().contains(ukey)))
      .toList();
    List<Reference> finalReferenceList = referenceList;
    Map<String, List<Attribute>> filteredSkuAttributes = new HashMap<>(skuAttributes.getSkuAttributesList());
    filteredSkuAttributes
      .forEach((key,value) ->
        value.removeIf(attr -> finalReferenceList.stream().noneMatch(ref -> ref.getAttrClasses().left().equals(attr.getUkey())))
      );
    if(config.getComplexMapping().getProducttypeAttrClassesToGroup() != null) {
      List<String> ukeyOrder = new ArrayList<>();
      for(ProductType productType : ctx.product().productTypes()){
        ukeyOrder.addAll(productType.objAttrs().stream()
          .filter(objAttr -> finalReferenceList.stream().anyMatch(reference -> reference.getId().equals(objAttr.attrId())))
          .map(ObjAttr::ukey)
          .toList());
      }

      filteredSkuAttributes.values().forEach(attrs ->
        attrs.sort(Comparator.comparingInt(attr -> {
          int idx = ukeyOrder.indexOf(attr.getUkey());
          return idx == -1 ? Integer.MAX_VALUE : idx;
        }))
      );
    }
    for (Map.Entry<String, List<Attribute>> entry : filteredSkuAttributes.entrySet()) {
      entry.getValue().forEach(attribute  -> {
        String value = extractText(attribute);
        if (result.containsKey(entry.getKey()) && result.get(entry.getKey()).containsKey(config.getTargetField()) && result.get(entry.getKey()).get(config.getTargetField()).getRight() instanceof Map) {
          Map<String,Object> resultMap = (Map<String, Object>) result.get(entry.getKey()).get(config.getTargetField()).getRight();
          resultMap.put(attribute.getUkey(), value);
          result.get(entry.getKey()).put(config.getTargetField(), Pair.of(config.getTargetFieldType(), resultMap));
        } else {
          Map<String,Object> valueMap = new HashMap<>();
          valueMap.put(attribute.getUkey(), value);
          if (result.containsKey(entry.getKey())) {
            result.get(entry.getKey()).put(config.getTargetField(), Pair.of(config.getTargetFieldType(), valueMap));
          } else {
            result.put(entry.getKey(), new HashMap<>(Map.of(config.getTargetField(), Pair.of(config.getTargetFieldType(), valueMap))));
          }
        }
      });
    }

    }

  }

