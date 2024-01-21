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

package cn.taketoday.http.codec.cbor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import org.reactivestreams.Publisher;

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.json.AbstractJackson2Encoder;
import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;

/**
 * Encode from an {@code Object} to bytes of CBOR objects using Jackson.
 * Stream encoding is not supported yet.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Jackson2CborDecoder
 * @since 4.0
 */
public class Jackson2CborEncoder extends AbstractJackson2Encoder {

  public Jackson2CborEncoder() {
    this(Jackson2ObjectMapperBuilder.cbor().build(), MediaType.APPLICATION_CBOR);
  }

  public Jackson2CborEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
    Assert.isAssignable(CBORFactory.class, mapper.getFactory().getClass());
  }

  @Override
  public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
          ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
    throw new UnsupportedOperationException("Does not support stream encoding yet");
  }

}
