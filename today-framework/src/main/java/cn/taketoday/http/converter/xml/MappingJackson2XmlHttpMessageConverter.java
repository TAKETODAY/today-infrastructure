/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http.converter.xml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.nio.charset.StandardCharsets;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.json.AbstractJackson2HttpMessageConverter;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Assert;

/**
 * Implementation of {@link HttpMessageConverter HttpMessageConverter}
 * that can read and write XML using <a href="https://github.com/FasterXML/jackson-dataformat-xml">
 * Jackson 2.x extension component for reading and writing XML encoded data</a>.
 *
 * <p>By default, this converter supports {@code application/xml}, {@code text/xml}, and
 * {@code application/*+xml} with {@code UTF-8} character set. This can be overridden by
 * setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
 *
 * <p>Compatible with Jackson 2.9 to 2.12.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/27 23:00
 */
public class MappingJackson2XmlHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

  /**
   * Construct a new {@code MappingJackson2XmlHttpMessageConverter} using default configuration
   * provided by {@code Jackson2ObjectMapperBuilder}.
   */
  public MappingJackson2XmlHttpMessageConverter() {
    this(Jackson2ObjectMapperBuilder.xml().build());
  }

  /**
   * Construct a new {@code MappingJackson2XmlHttpMessageConverter} with a custom {@link ObjectMapper}
   * (must be a {@link XmlMapper} instance).
   * You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
   *
   * @see Jackson2ObjectMapperBuilder#xml()
   */
  public MappingJackson2XmlHttpMessageConverter(ObjectMapper objectMapper) {
    super(objectMapper, new MediaType("application", "xml", StandardCharsets.UTF_8),
            new MediaType("text", "xml", StandardCharsets.UTF_8),
            new MediaType("application", "*+xml", StandardCharsets.UTF_8));
    Assert.isInstanceOf(XmlMapper.class, objectMapper, "XmlMapper required");
  }

  /**
   * {@inheritDoc}
   * The {@code ObjectMapper} parameter must be a {@link XmlMapper} instance.
   */
  @Override
  public void setObjectMapper(ObjectMapper objectMapper) {
    Assert.isInstanceOf(XmlMapper.class, objectMapper, "XmlMapper required");
    super.setObjectMapper(objectMapper);
  }

}

