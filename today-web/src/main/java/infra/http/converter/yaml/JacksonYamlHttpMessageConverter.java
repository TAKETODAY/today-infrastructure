/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.converter.yaml;

import infra.http.MediaType;
import infra.http.converter.AbstractJacksonHttpMessageConverter;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Implementation of {@link infra.http.converter.HttpMessageConverter
 * HttpMessageConverter} that can read and write the <a href="https://yaml.io/">YAML</a>
 * data format using <a href="https://github.com/FasterXML/jackson-dataformats-text/tree/3.x/yaml">
 * the dedicated Jackson 3.x extension</a>.
 *
 * <p>By default, this converter supports the {@link MediaType#APPLICATION_YAML_VALUE}
 * media type. This can be overridden by setting the {@link #setSupportedMediaTypes
 * supportedMediaTypes} property.
 *
 * <p>The following hints entries are supported:
 * <ul>
 *     <li>A JSON view with a <code>"com.fasterxml.jackson.annotation.JsonView"</code>
 *         key and the class name of the JSON view as value.</li>
 *     <li>A filter provider with a <code>"tools.jackson.databind.ser.FilterProvider"</code>
 *         key and the filter provider class name as value.</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class JacksonYamlHttpMessageConverter extends AbstractJacksonHttpMessageConverter<YAMLMapper> {

  /**
   * Construct a new instance with a {@link YAMLMapper} customized with the
   * {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)}.
   */
  public JacksonYamlHttpMessageConverter() {
    super(YAMLMapper.builder(), MediaType.APPLICATION_YAML);
  }

  /**
   * Construct a new instance with the provided {@link YAMLMapper}.
   *
   * @see YAMLMapper#builder()
   */
  public JacksonYamlHttpMessageConverter(YAMLMapper.Builder builder) {
    super(builder, MediaType.APPLICATION_YAML);
  }

  /**
   * Construct a new instance with the provided {@link YAMLMapper.Builder} customized
   * with the {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)}.
   *
   * @see YAMLMapper#builder()
   */
  public JacksonYamlHttpMessageConverter(YAMLMapper mapper) {
    super(mapper, MediaType.APPLICATION_YAML);
  }

}
