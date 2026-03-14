package de.kittelberger.illusion.util;

import de.kittelberger.bosch.pt.SeoUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class UrlBuilderUtil {

  private static final String DEFAULT_PRODUCT_SCHEME = "/{country}/{language}/products/{seoifiedName}-{Id}";
  private String productScheme = DEFAULT_PRODUCT_SCHEME;

  private String mediaObjectScheme = "/binary/ocsmedia/optimized/full/{mediaObjectFilename}.webp";

  public String getProductUrl(String productName, String productSKU, String protocolAndDomain, String ocsContextName, String country, String language) {
    String returnUrl = protocolAndDomain;
    if (StringUtils.isNotBlank(ocsContextName)) {
      returnUrl = returnUrl.concat("/" + ocsContextName);
    }
    returnUrl = returnUrl.concat(productScheme);
    returnUrl = replaceCountryAndLanguage(returnUrl, country.toLowerCase(), language.toLowerCase());
    returnUrl = replaceSeoifiedName(returnUrl, country.toLowerCase(), language.toLowerCase(), productName);
    returnUrl = replaceId(returnUrl, productSKU.trim().replace(" ",""));
    return returnUrl;
  }

  private String replaceId(String input, String id) {
    return input.replace("{Id}", id);
  }

  private String replaceCountryAndLanguage(String input, String country, String language) {
    return input.replace("{country}", country.toLowerCase()).replace("{language}", language.toLowerCase());
  }

  private String replaceSeoifiedName(final String input, final String country, final String language, final String name) {
    String seofiedName = SeoUtil.seofyString(country, language, name);
    if (seofiedName.endsWith("-")) {
      String sub = seofiedName.substring(0, seofiedName.length() -1);
      seofiedName = sub;
    }
    return input.replace("{seoifiedName}", seofiedName);
  }

  private String replaceMediaObjectFilename(String input, String mediaObjectFilename) {
    return input.replace("{mediaObjectFilename}", mediaObjectFilename);
  }

  public static String replaceOldDomainOrSetWithNewDomain(final String originalString, final String partWhichShouldBeReplaced, final String partWhichWillReplaceWith) {
    String returnString = originalString;
    if (StringUtils.isNotBlank(originalString) && StringUtils.isNotBlank(partWhichShouldBeReplaced) && StringUtils.isNotBlank(partWhichWillReplaceWith)) {
      returnString = originalString.replace(partWhichShouldBeReplaced, partWhichWillReplaceWith);
    }

    if (StringUtils.startsWith(returnString, "/") && StringUtils.isNotBlank(partWhichWillReplaceWith)) {
      returnString = partWhichWillReplaceWith + returnString;
    }

    return returnString;
  }

  public String getMediaObjectUrl(String mediaObjectFilename, String protocolAndDomain) {
    String returnUrl = protocolAndDomain.concat(this.mediaObjectScheme);
    returnUrl = this.replaceMediaObjectFilename(returnUrl, mediaObjectFilename);
    replaceOldDomainOrSetWithNewDomain(returnUrl, "", protocolAndDomain);
    return returnUrl;
  }

}
