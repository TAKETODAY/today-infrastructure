/*
 * Copyright 2017 - 2026 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.annotation.config.jackson;

import java.util.EnumMap;
import java.util.Map;

import infra.context.properties.ConfigurationProperties;
import tools.jackson.dataformat.xml.XmlReadFeature;
import tools.jackson.dataformat.xml.XmlWriteFeature;

/**
 * Configuration properties to configure Jackson's XML support.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@ConfigurationProperties("jackson.xml")
public class JacksonXmlProperties {

  /**
   * Jackson on/off token reader features that are specific to XML.
   */
  private final Map<XmlReadFeature, Boolean> read = new EnumMap<>(XmlReadFeature.class);

  /**
   * Jackson on/off token writer features that are specific to XML.
   */
  private final Map<XmlWriteFeature, Boolean> write = new EnumMap<>(XmlWriteFeature.class);

  public Map<XmlReadFeature, Boolean> getRead() {
    return this.read;
  }

  public Map<XmlWriteFeature, Boolean> getWrite() {
    return this.write;
  }

}
