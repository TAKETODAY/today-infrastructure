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
import tools.jackson.dataformat.cbor.CBORReadFeature;
import tools.jackson.dataformat.cbor.CBORWriteFeature;

/**
 * Configuration properties to configure Jackson's CBOR support.
 *
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Johannes Edmeier
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@ConfigurationProperties("jackson.cbor")
public class JacksonCborProperties {

  /**
   * Jackson on/off token reader features that are specific to CBOR.
   */
  private final Map<CBORReadFeature, Boolean> read = new EnumMap<>(CBORReadFeature.class);

  /**
   * Jackson on/off token writer features that are specific to CBOR.
   */
  private final Map<CBORWriteFeature, Boolean> write = new EnumMap<>(CBORWriteFeature.class);

  public Map<CBORReadFeature, Boolean> getRead() {
    return this.read;
  }

  public Map<CBORWriteFeature, Boolean> getWrite() {
    return this.write;
  }

}
