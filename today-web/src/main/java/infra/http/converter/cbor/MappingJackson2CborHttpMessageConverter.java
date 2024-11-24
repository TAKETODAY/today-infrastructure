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

package infra.http.converter.cbor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import infra.http.MediaType;
import infra.http.converter.json.AbstractJackson2HttpMessageConverter;
import infra.http.converter.json.Jackson2ObjectMapperBuilder;
import infra.lang.Assert;

/**
 * Implementation of {@link infra.http.converter.HttpMessageConverter
 * HttpMessageConverter} that can read and write the <a href="https://cbor.io/">CBOR</a>
 * data format using <a href="https://github.com/FasterXML/jackson-dataformats-binary/tree/master/cbor">
 * the dedicated Jackson 2.x extension</a>.
 *
 * <p>By default, this converter supports the {@link MediaType#APPLICATION_CBOR_VALUE}
 * media type. This can be overridden by setting the {@link #setSupportedMediaTypes
 * supportedMediaTypes} property.
 *
 * <p>The default constructor uses the default configuration provided by
 * {@link Jackson2ObjectMapperBuilder}.
 *
 * <p>Compatible with Jackson 2.9 to 2.12
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class MappingJackson2CborHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

  /**
   * Construct a new {@code MappingJackson2CborHttpMessageConverter} using the
   * default configuration provided by {@code Jackson2ObjectMapperBuilder}.
   */
  public MappingJackson2CborHttpMessageConverter() {
    this(Jackson2ObjectMapperBuilder.cbor().build());
  }

  /**
   * Construct a new {@code MappingJackson2CborHttpMessageConverter} with a
   * custom {@link ObjectMapper} (must be configured with a {@code CBORFactory}
   * instance).
   * <p>You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
   *
   * @see Jackson2ObjectMapperBuilder#cbor()
   */
  public MappingJackson2CborHttpMessageConverter(ObjectMapper objectMapper) {
    super(objectMapper, MediaType.APPLICATION_CBOR);
    Assert.isInstanceOf(CBORFactory.class, objectMapper.getFactory(), "CBORFactory required");
  }

  /**
   * {@inheritDoc}
   * The {@code ObjectMapper} must be configured with a {@code CBORFactory} instance.
   */
  @Override
  public void setObjectMapper(ObjectMapper objectMapper) {
    Assert.isInstanceOf(CBORFactory.class, objectMapper.getFactory(), "CBORFactory required");
    super.setObjectMapper(objectMapper);
  }

}
