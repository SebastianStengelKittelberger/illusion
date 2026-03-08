package de.kittelberger.illusion.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Image {
  private String fileName;
  private String url;
  private String ukey;
  private Long size;

}
