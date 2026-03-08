package de.kittelberger.illusion.model;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ProductMetaData extends MetaData
{
  public ProductMetaData(String name, Long id) {
    super(name, id);
  }

  public ProductMetaData(String name, Long id, String artNo) {
    this(name, id);
    this.artNo = artNo;
  }

  private String artNo;
}
