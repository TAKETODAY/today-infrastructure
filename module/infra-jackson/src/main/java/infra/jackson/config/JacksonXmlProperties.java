/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jackson.config;

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
