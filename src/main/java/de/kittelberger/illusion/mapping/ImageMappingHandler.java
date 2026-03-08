package de.kittelberger.illusion.mapping;

import de.kittelberger.illusion.model.*;
import de.kittelberger.illusion.util.UrlBuilderUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@Order(20)
public class ImageMappingHandler implements MappingHandler{

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
    Map<String, Pair<String, Object>> result) {
    if(!"IMAGE".equals(config.getTargetFieldType())) {
      return;
    }

    SkuAttributes skuAttributes = ctx.skuAttributes();
    Optional<Attribute> attribute = skuAttributes.getFirstAttribute(config.getUkey());
    if(attribute.isPresent()) {
       MediaObject mediaObject = ctx.mediaObjectMap().get(attribute.get().getReferenceIds().get("mediaObjectId"));
       if(mediaObject != null) {
         Image image = Image.builder()
           .fileName(mediaObject.getName())
           .size(mediaObject.getMediaSpecifics().stream().map(MediaSpecifics::getSize).filter(Objects::nonNull).findFirst().orElse(0L))
           .url(urlBuilderUtil.getMediaObjectUrl(mediaObject.getName(), ctx.domain()))
           .ukey(attribute.get().getUkey())
           .build();
         result.put(config.getTargetField(), Pair.of(config.getTargetFieldType(), image));
       }
    }

  }
}
