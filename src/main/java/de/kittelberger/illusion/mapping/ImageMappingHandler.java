package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.*;
import de.kittelberger.illusion.util.UrlBuilderUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Order(20)
public class ImageMappingHandler implements MappingHandler {

  private final UrlBuilderUtil urlBuilderUtil;

  public ImageMappingHandler(UrlBuilderUtil urlBuilderUtil) {
    this.urlBuilderUtil = urlBuilderUtil;
  }

  @Override
  public boolean supports(MapConfig config) {
    return "IMAGE".equals(config.getMappingType());
  }

  @Override
  public void apply(
    final MapConfig config,
    final MappingContext ctx,
    Map<String, Map<String, Pair<String, Object>>> result) {
    if (!"IMAGE".equals(config.getTargetFieldType())) {
      return;
    }

    SkuAttributes skuAttributes = ctx.skuAttributes();
    for (String key : skuAttributes.getSkuAttributesList().keySet()) {
      Optional<Attribute> attribute = skuAttributes.getFirstAttribute(key, config.getUkey());
      if (attribute.isPresent() && attribute.get().getReferenceIds() != null && attribute.get().getReferenceIds().containsKey("mediaObjectId")) {
        MediaObject mediaObject = ctx.mediaObjectMap().get(attribute.get().getReferenceIds().get("mediaObjectId"));
        if (mediaObject != null) {
          Image image = Image.builder()
            .fileName(mediaObject.getName())
            .size(mediaObject.getMediaSpecifics().stream().map(MediaSpecifics::getSize).filter(Objects::nonNull).findFirst().orElse(0L))
            .url(urlBuilderUtil.getMediaObjectUrl(mediaObject.getName(), ctx.domain()))
            .ukey(attribute.get().getUkey())
            .build();
          if (result.containsKey(key)) {
            result.get(key).put(config.getTargetField(), Pair.of(config.getTargetFieldType(), image));
          } else {
            result.put(key, new HashMap<>(Map.of(config.getTargetField(), Pair.of(config.getTargetFieldType(), image))));
          }
        }
      }

    }
  }
}
