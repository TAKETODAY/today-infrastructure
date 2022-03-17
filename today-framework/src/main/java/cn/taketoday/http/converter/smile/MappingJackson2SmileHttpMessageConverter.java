/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.converter.smile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import cn.taketoday.http.converter.json.AbstractJackson2HttpMessageConverter;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.http.MediaType;

/**
 * Implementation of {@link cn.taketoday.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write Smile data format ("binary JSON") using
 * <a href="https://github.com/FasterXML/jackson-dataformats-binary/tree/master/smile">
 * the dedicated Jackson 2.x extension</a>.
 *
 * <p>By default, this converter supports {@code "application/x-jackson-smile"} media type.
 * This can be overridden by setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
 *
 * <p>Compatible with Jackson 2.9 to 2.12
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class MappingJackson2SmileHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

  /**
   * Construct a new {@code MappingJackson2SmileHttpMessageConverter} using default configuration
   * provided by {@code Jackson2ObjectMapperBuilder}.
   */
  public MappingJackson2SmileHttpMessageConverter() {
    this(Jackson2ObjectMapperBuilder.smile().build());
  }

  /**
   * Construct a new {@code MappingJackson2SmileHttpMessageConverter} with a custom {@link ObjectMapper}
   * (must be configured with a {@code SmileFactory} instance).
   * You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
   *
   * @see Jackson2ObjectMapperBuilder#smile()
   */
  public MappingJackson2SmileHttpMessageConverter(ObjectMapper objectMapper) {
    super(objectMapper, new MediaType("application", "x-jackson-smile"));
    Assert.isInstanceOf(SmileFactory.class, objectMapper.getFactory(), "SmileFactory required");
  }

  /**
   * {@inheritDoc}
   * The {@code ObjectMapper} must be configured with a {@code SmileFactory} instance.
   */
  @Override
  public void setObjectMapper(ObjectMapper objectMapper) {
    Assert.isInstanceOf(SmileFactory.class, objectMapper.getFactory(), "SmileFactory required");
    super.setObjectMapper(objectMapper);
  }

}
