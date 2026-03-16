package de.kittelberger.illusion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Image {
  private String fileName;
  private String url;
  private String ukey;
  private Long size;
  private String seoImageId;
  private ImageDimension imageDimension;
  private Double aspectRatio;
  private String title;
  private String altText;
  private Long mediaItemId;
  private Long mediaObjectId;
}
