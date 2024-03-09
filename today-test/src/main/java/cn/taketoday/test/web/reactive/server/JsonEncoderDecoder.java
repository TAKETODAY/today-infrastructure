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

package cn.taketoday.test.web.reactive.server;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.DecoderHttpMessageReader;
import cn.taketoday.http.codec.EncoderHttpMessageWriter;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.lang.Nullable;

/**
 * {@link Encoder} and {@link Decoder} that is able to handle a map to and from
 * JSON. Used to configure the jsonpath infrastructure without having a hard
 * dependency on the library.
 *
 * @param encoder the JSON encoder
 * @param decoder the JSON decoder
 * @author Stephane Nicoll
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
record JsonEncoderDecoder(Encoder<?> encoder, Decoder<?> decoder) {

  private static final ResolvableType MAP_TYPE = ResolvableType.forClass(Map.class);

  /**
   * Create a {@link JsonEncoderDecoder} instance based on the specified
   * infrastructure.
   *
   * @param messageWriters the HTTP message writers
   * @param messageReaders the HTTP message readers
   * @return a {@link JsonEncoderDecoder} or {@code null} if a suitable codec
   * is not available
   */
  @Nullable
  static JsonEncoderDecoder from(Collection<HttpMessageWriter<?>> messageWriters,
          Collection<HttpMessageReader<?>> messageReaders) {

    Encoder<?> jsonEncoder = findJsonEncoder(messageWriters);
    Decoder<?> jsonDecoder = findJsonDecoder(messageReaders);
    if (jsonEncoder != null && jsonDecoder != null) {
      return new JsonEncoderDecoder(jsonEncoder, jsonDecoder);
    }
    return null;
  }

  /**
   * Find the first suitable {@link Encoder} that can encode a {@link Map}
   * to JSON.
   *
   * @param writers the writers to inspect
   * @return a suitable JSON {@link Encoder} or {@code null}
   */
  @Nullable
  private static Encoder<?> findJsonEncoder(Collection<HttpMessageWriter<?>> writers) {
    return findJsonEncoder(writers.stream()
            .filter(EncoderHttpMessageWriter.class::isInstance)
            .map(writer -> ((EncoderHttpMessageWriter<?>) writer).getEncoder()));
  }

  @Nullable
  private static Encoder<?> findJsonEncoder(Stream<Encoder<?>> stream) {
    return stream
            .filter(encoder -> encoder.canEncode(MAP_TYPE, MediaType.APPLICATION_JSON))
            .findFirst()
            .orElse(null);
  }

  /**
   * Find the first suitable {@link Decoder} that can decode a {@link Map} to
   * JSON.
   *
   * @param readers the readers to inspect
   * @return a suitable JSON {@link Decoder} or {@code null}
   */
  @Nullable
  private static Decoder<?> findJsonDecoder(Collection<HttpMessageReader<?>> readers) {
    return findJsonDecoder(readers.stream()
            .filter(DecoderHttpMessageReader.class::isInstance)
            .map(reader -> ((DecoderHttpMessageReader<?>) reader).getDecoder()));
  }

  @Nullable
  private static Decoder<?> findJsonDecoder(Stream<Decoder<?>> decoderStream) {
    return decoderStream
            .filter(decoder -> decoder.canDecode(MAP_TYPE, MediaType.APPLICATION_JSON))
            .findFirst()
            .orElse(null);
  }

}
