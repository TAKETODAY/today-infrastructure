/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.http.converter.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.json.AbstractJackson2HttpMessageConverter;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Assert;

/**
 * Implementation of {@link cn.taketoday.http.converter.HttpMessageConverter
 * HttpMessageConverter} that can read and write the <a href="https://yaml.io/">YAML</a>
 * data format using <a href="https://github.com/FasterXML/jackson-dataformats-text/tree/2.17/yaml">
 * the dedicated Jackson 2.x extension</a>.
 *
 * <p>By default, this converter supports the {@link MediaType#APPLICATION_YAML_VALUE}
 * media type. This can be overridden by setting the {@link #setSupportedMediaTypes
 * supportedMediaTypes} property.
 *
 * <p>The default constructor uses the default configuration provided by
 * {@link Jackson2ObjectMapperBuilder}.
 *
 * @author Hyoungjune Kim
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MappingJackson2YamlHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

  /**
   * Construct a new {@code MappingJackson2YamlHttpMessageConverter} using the
   * default configuration provided by {@code Jackson2ObjectMapperBuilder}.
   */
  public MappingJackson2YamlHttpMessageConverter() {
    this(Jackson2ObjectMapperBuilder.yaml().build());
  }

  /**
   * Construct a new {@code MappingJackson2YamlHttpMessageConverter} with a
   * custom {@link ObjectMapper} (must be configured with a {@code YAMLFactory}
   * instance).
   * <p>You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
   *
   * @see Jackson2ObjectMapperBuilder#yaml()
   */
  public MappingJackson2YamlHttpMessageConverter(ObjectMapper objectMapper) {
    super(objectMapper, MediaType.APPLICATION_YAML);
    Assert.isInstanceOf(YAMLFactory.class, objectMapper.getFactory(), "YAMLFactory required");
  }

  /**
   * {@inheritDoc}
   * <p>The {@code ObjectMapper} must be configured with a {@code YAMLFactory} instance.
   */
  @Override
  public void setObjectMapper(ObjectMapper objectMapper) {
    Assert.isInstanceOf(YAMLFactory.class, objectMapper.getFactory(), "YAMLFactory required");
    super.setObjectMapper(objectMapper);
  }

}
