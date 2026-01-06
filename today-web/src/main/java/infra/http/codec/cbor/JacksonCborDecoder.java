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

package infra.http.codec.cbor;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.http.MediaType;
import infra.http.codec.AbstractJacksonDecoder;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.cbor.CBORMapper;

/**
 * Decode bytes into CBOR and convert to Objects with Jackson 3.x.
 *
 * <p>Stream decoding is currently not supported.
 *
 * @author Sebastien Deleuze
 * @see JacksonCborEncoder
 * @since 5.0
 */
public class JacksonCborDecoder extends AbstractJacksonDecoder<CBORMapper> {

  /**
   * Construct a new instance with a {@link CBORMapper} customized with the
   * {@link tools.jackson.databind.JacksonModule}s found by
   * {@link MapperBuilder#findModules(ClassLoader)}.
   */
  public JacksonCborDecoder() {
    super(CBORMapper.builder(), MediaType.APPLICATION_CBOR);
  }

  /**
   * Construct a new instance with the provided {@link CBORMapper.Builder}
   * customized with the {@link tools.jackson.databind.JacksonModule}s
   * found by {@link MapperBuilder#findModules(ClassLoader)}.
   *
   * @see CBORMapper#builder()
   */
  public JacksonCborDecoder(CBORMapper.Builder builder) {
    super(builder, MediaType.APPLICATION_CBOR);
  }

  /**
   * Construct a new instance with the provided {@link CBORMapper}.
   *
   * @see CBORMapper#builder()
   */
  public JacksonCborDecoder(CBORMapper mapper) {
    super(mapper, MediaType.APPLICATION_CBOR);
  }

  /**
   * Construct a new instance with the provided {@link CBORMapper.Builder}
   * customized with the {@link tools.jackson.databind.JacksonModule}s
   * found by {@link MapperBuilder#findModules(ClassLoader)}, and
   * {@link MimeType}s.
   *
   * @see CBORMapper#builder()
   */
  public JacksonCborDecoder(CBORMapper.Builder builder, MimeType... mimeTypes) {
    super(builder, mimeTypes);
  }

  /**
   * Construct a new instance with the provided {@link CBORMapper} and {@link MimeType}s.
   *
   * @see CBORMapper#builder()
   */
  public JacksonCborDecoder(CBORMapper mapper, MimeType... mimeTypes) {
    super(mapper, mimeTypes);
  }

  @Override
  public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    throw new UnsupportedOperationException("Stream decoding is currently not supported");
  }

}
