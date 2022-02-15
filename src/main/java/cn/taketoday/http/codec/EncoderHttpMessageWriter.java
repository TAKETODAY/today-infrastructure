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

package cn.taketoday.http.codec;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.AbstractEncoder;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.PooledDataBuffer;
import cn.taketoday.http.HttpLogging;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@code HttpMessageWriter} that wraps and delegates to an {@link Encoder}.
 *
 * <p>Also a {@code HttpMessageWriter} that pre-resolves encoding hints
 * from the extra information available on the server side such as the request
 * or controller method annotations.
 *
 * @param <T> the type of objects in the input stream
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.0
 */
public class EncoderHttpMessageWriter<T> implements HttpMessageWriter<T> {
  private static final Logger logger = HttpLogging.forLogName(EncoderHttpMessageWriter.class);

  private final Encoder<T> encoder;
  private final List<MediaType> mediaTypes;

  @Nullable
  private final MediaType defaultMediaType;

  /**
   * Create an instance wrapping the given {@link Encoder}.
   */
  public EncoderHttpMessageWriter(Encoder<T> encoder) {
    Assert.notNull(encoder, "Encoder is required");
    initLogger(encoder);
    this.encoder = encoder;
    this.mediaTypes = MediaType.asMediaTypes(encoder.getEncodableMimeTypes());
    this.defaultMediaType = initDefaultMediaType(this.mediaTypes);
  }

  private static void initLogger(Encoder<?> encoder) {
    if (encoder instanceof AbstractEncoder &&
            encoder.getClass().getName().startsWith("cn.taketoday.core.codec")) {
      Logger logger = HttpLogging.forLog(((AbstractEncoder<?>) encoder).getLogger());
      ((AbstractEncoder<?>) encoder).setLogger(logger);
    }
  }

  @Nullable
  private static MediaType initDefaultMediaType(List<MediaType> mediaTypes) {
    return mediaTypes.stream().filter(MediaType::isConcrete).findFirst().orElse(null);
  }

  /**
   * Return the {@code Encoder} of this writer.
   */
  public Encoder<T> getEncoder() {
    return this.encoder;
  }

  @Override
  public List<MediaType> getWritableMediaTypes() {
    return this.mediaTypes;
  }

  @Override
  public List<MediaType> getWritableMediaTypes(ResolvableType elementType) {
    return MediaType.asMediaTypes(getEncoder().getEncodableMimeTypes(elementType));
  }

  @Override
  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    return this.encoder.canEncode(elementType, mediaType);
  }

  @Override
  public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
                          @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

    MediaType contentType = updateContentType(message, mediaType);

    Flux<DataBuffer> body = this.encoder.encode(
            inputStream, message.bufferFactory(), elementType, contentType, hints);

    if (inputStream instanceof Mono) {
      return body
              .singleOrEmpty()
              .switchIfEmpty(Mono.defer(() -> {
                message.getHeaders().setContentLength(0);
                return message.setComplete().then(Mono.empty());
              }))
              .flatMap(buffer -> {
                Hints.touchDataBuffer(buffer, hints, logger);
                message.getHeaders().setContentLength(buffer.readableByteCount());
                return message.writeWith(
                        Mono.just(buffer)
                                .doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
              })
              .doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
    }

    if (isStreamingMediaType(contentType)) {
      return message.writeAndFlushWith(body.map(buffer -> {
        Hints.touchDataBuffer(buffer, hints, logger);
        return Mono.just(buffer).doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
      }));
    }

    if (logger.isDebugEnabled()) {
      body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
    }
    return message.writeWith(body);
  }

  @Nullable
  private MediaType updateContentType(ReactiveHttpOutputMessage message, @Nullable MediaType mediaType) {
    MediaType result = message.getHeaders().getContentType();
    if (result != null) {
      return result;
    }
    MediaType fallback = this.defaultMediaType;
    result = (useFallback(mediaType, fallback) ? fallback : mediaType);
    if (result != null) {
      result = addDefaultCharset(result, fallback);
      message.getHeaders().setContentType(result);
    }
    return result;
  }

  private static boolean useFallback(@Nullable MediaType main, @Nullable MediaType fallback) {
    return (main == null || !main.isConcrete() ||
            main.equals(MediaType.APPLICATION_OCTET_STREAM) && fallback != null);
  }

  private static MediaType addDefaultCharset(MediaType main, @Nullable MediaType defaultType) {
    if (main.getCharset() == null && defaultType != null && defaultType.getCharset() != null) {
      return new MediaType(main, defaultType.getCharset());
    }
    return main;
  }

  private boolean isStreamingMediaType(@Nullable MediaType mediaType) {
    if (mediaType == null || !(this.encoder instanceof HttpMessageEncoder)) {
      return false;
    }
    for (MediaType streamingMediaType : ((HttpMessageEncoder<?>) this.encoder).getStreamingMediaTypes()) {
      if (mediaType.isCompatibleWith(streamingMediaType) && matchParameters(mediaType, streamingMediaType)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchParameters(MediaType streamingMediaType, MediaType mediaType) {
    for (String name : streamingMediaType.getParameters().keySet()) {
      String s1 = streamingMediaType.getParameter(name);
      String s2 = mediaType.getParameter(name);
      if (StringUtils.hasText(s1) && StringUtils.hasText(s2) && !s1.equalsIgnoreCase(s2)) {
        return false;
      }
    }
    return true;
  }

  // Server side only...

  @Override
  public Mono<Void> write(
          Publisher<? extends T> inputStream, ResolvableType actualType,
          ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request,
          ServerHttpResponse response, Map<String, Object> hints) {

    Map<String, Object> allHints = Hints.merge(
            hints, getWriteHints(actualType, elementType, mediaType, request, response));

    return write(inputStream, elementType, mediaType, response, allHints);
  }

  /**
   * Get additional hints for encoding for example based on the server request
   * or annotations from controller method parameters. By default, delegate to
   * the encoder if it is an instance of {@link HttpMessageEncoder}.
   */
  protected Map<String, Object> getWriteHints(
          ResolvableType streamType, ResolvableType elementType,
          @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

    if (this.encoder instanceof HttpMessageEncoder<?> encoder) {
      return encoder.getEncodeHints(streamType, elementType, mediaType, request, response);
    }
    return Hints.none();
  }

}
