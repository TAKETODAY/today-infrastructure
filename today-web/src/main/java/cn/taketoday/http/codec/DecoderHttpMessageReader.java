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

package cn.taketoday.http.codec;

import java.util.List;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.AbstractDecoder;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.http.HttpLogging;
import cn.taketoday.http.HttpMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@code HttpMessageReader} that wraps and delegates to a {@link Decoder}.
 *
 * <p>Also a {@code HttpMessageReader} that pre-resolves decoding hints
 * from the extra information available on the server side such as the request
 * or controller method parameter annotations.
 *
 * @param <T> the type of objects in the decoded output stream
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DecoderHttpMessageReader<T> implements HttpMessageReader<T> {

  private final Decoder<T> decoder;

  private final List<MediaType> mediaTypes;

  /**
   * Create an instance wrapping the given {@link Decoder}.
   */
  public DecoderHttpMessageReader(Decoder<T> decoder) {
    Assert.notNull(decoder, "Decoder is required");
    initLogger(decoder);
    this.decoder = decoder;
    this.mediaTypes = MediaType.asMediaTypes(decoder.getDecodableMimeTypes());
  }

  private static void initLogger(Decoder<?> decoder) {
    if (decoder instanceof AbstractDecoder &&
            decoder.getClass().getName().startsWith("cn.taketoday.core.codec")) {
      Logger logger = HttpLogging.forLog(((AbstractDecoder<?>) decoder).getLogger());
      ((AbstractDecoder<?>) decoder).setLogger(logger);
    }
  }

  /**
   * Return the {@link Decoder} of this reader.
   */
  public Decoder<T> getDecoder() {
    return this.decoder;
  }

  @Override
  public List<MediaType> getReadableMediaTypes() {
    return this.mediaTypes;
  }

  @Override
  public List<MediaType> getReadableMediaTypes(ResolvableType elementType) {
    return MediaType.asMediaTypes(this.decoder.getDecodableMimeTypes(elementType));
  }

  @Override
  public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
    return this.decoder.canDecode(elementType, mediaType);
  }

  @Override
  public Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
    MediaType contentType = getContentType(message);
    Map<String, Object> allHints = Hints.merge(hints, getReadHints(elementType, message));
    return this.decoder.decode(message.getBody(), elementType, contentType, allHints);
  }

  @Override
  public Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
    MediaType contentType = getContentType(message);
    Map<String, Object> allHints = Hints.merge(hints, getReadHints(elementType, message));
    return this.decoder.decodeToMono(message.getBody(), elementType, contentType, allHints);
  }

  /**
   * Determine the Content-Type of the HTTP message based on the
   * "Content-Type" header or otherwise default to
   * {@link MediaType#APPLICATION_OCTET_STREAM}.
   *
   * @param inputMessage the HTTP message
   * @return the MediaType, possibly {@code null}.
   */
  @Nullable
  protected MediaType getContentType(HttpMessage inputMessage) {
    MediaType contentType = inputMessage.getHeaders().getContentType();
    return contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM;
  }

  /**
   * Get additional hints for decoding based on the input HTTP message.
   */
  protected Map<String, Object> getReadHints(ResolvableType elementType, ReactiveHttpInputMessage message) {
    return Hints.none();
  }

  // Server-side only...

  @Override
  public Flux<T> read(ResolvableType actualType, ResolvableType elementType,
          ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

    Map<String, Object> allHints = Hints.merge(
            hints, getReadHints(actualType, elementType, request, response));

    return read(elementType, request, allHints);
  }

  @Override
  public Mono<T> readMono(ResolvableType actualType, ResolvableType elementType,
          ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

    Map<String, Object> allHints = Hints.merge(
            hints, getReadHints(actualType, elementType, request, response));

    return readMono(elementType, request, allHints);
  }

  /**
   * Get additional hints for decoding for example based on the server request
   * or annotations from controller method parameters. By default, delegate to
   * the decoder if it is an instance of {@link HttpMessageDecoder}.
   */
  protected Map<String, Object> getReadHints(ResolvableType actualType,
          ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response) {

    if (this.decoder instanceof HttpMessageDecoder<?> decoder) {
      return decoder.getDecodeHints(actualType, elementType, request, response);
    }
    return Hints.none();
  }

}
