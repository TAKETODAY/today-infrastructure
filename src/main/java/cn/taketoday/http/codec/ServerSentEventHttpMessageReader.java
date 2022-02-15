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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.CodecException;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reader that supports a stream of {@link ServerSentEvent ServerSentEvents} and also plain
 * {@link Object Objects} which is the same as an {@link ServerSentEvent} with data only.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServerSentEventHttpMessageReader implements HttpMessageReader<Object> {
  private static final ResolvableType STRING_TYPE = ResolvableType.fromClass(String.class);

  @Nullable
  private final Decoder<?> decoder;

  private final StringDecoder lineDecoder = StringDecoder.textPlainOnly();

  /**
   * Constructor without a {@code Decoder}. In this mode only {@code String}
   * is supported as the data of an event.
   */
  public ServerSentEventHttpMessageReader() {
    this(null);
  }

  /**
   * Constructor with JSON {@code Decoder} for decoding to Objects.
   * Support for decoding to {@code String} event data is built-in.
   */
  public ServerSentEventHttpMessageReader(@Nullable Decoder<?> decoder) {
    this.decoder = decoder;
  }

  /**
   * Return the configured {@code Decoder}.
   */
  @Nullable
  public Decoder<?> getDecoder() {
    return this.decoder;
  }

  /**
   * Configure a limit on the maximum number of bytes per SSE event which are
   * buffered before the event is parsed.
   * <p>Note that the {@link #getDecoder() data decoder}, if provided, must
   * also be customized accordingly to raise the limit if necessary in order
   * to be able to parse the data portion of the event.
   * <p>By default this is set to 256K.
   *
   * @param byteCount the max number of bytes to buffer, or -1 for unlimited
   */
  public void setMaxInMemorySize(int byteCount) {
    this.lineDecoder.setMaxInMemorySize(byteCount);
  }

  /**
   * Return the {@link #setMaxInMemorySize configured} byte count limit.
   */
  public int getMaxInMemorySize() {
    return this.lineDecoder.getMaxInMemorySize();
  }

  @Override
  public List<MediaType> getReadableMediaTypes() {
    return Collections.singletonList(MediaType.TEXT_EVENT_STREAM);
  }

  @Override
  public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
    return (MediaType.TEXT_EVENT_STREAM.includes(mediaType) || isServerSentEvent(elementType));
  }

  private boolean isServerSentEvent(ResolvableType elementType) {
    return ServerSentEvent.class.isAssignableFrom(elementType.toClass());
  }

  @Override
  public Flux<Object> read(
          ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {

    LimitTracker limitTracker = new LimitTracker();

    boolean shouldWrap = isServerSentEvent(elementType);
    ResolvableType valueType = (shouldWrap ? elementType.getGeneric() : elementType);

    return this.lineDecoder.decode(message.getBody(), STRING_TYPE, null, hints)
            .doOnNext(limitTracker::afterLineParsed)
            .bufferUntil(String::isEmpty)
            .concatMap(lines -> {
              Object event = buildEvent(lines, valueType, shouldWrap, hints);
              return (event != null ? Mono.just(event) : Mono.empty());
            });
  }

  @Nullable
  private Object buildEvent(
          List<String> lines, ResolvableType valueType, boolean shouldWrap, Map<String, Object> hints) {

    ServerSentEvent.Builder<Object> sseBuilder = shouldWrap ? ServerSentEvent.builder() : null;
    StringBuilder data = null;
    StringBuilder comment = null;

    for (String line : lines) {
      if (line.startsWith("data:")) {
        int length = line.length();
        if (length > 5) {
          int index = (line.charAt(5) != ' ' ? 5 : 6);
          if (length > index) {
            data = (data != null ? data : new StringBuilder());
            data.append(line, index, line.length());
            data.append('\n');
          }
        }
      }
      else if (shouldWrap) {
        if (line.startsWith("id:")) {
          sseBuilder.id(line.substring(3).trim());
        }
        else if (line.startsWith("event:")) {
          sseBuilder.event(line.substring(6).trim());
        }
        else if (line.startsWith("retry:")) {
          sseBuilder.retry(Duration.ofMillis(Long.parseLong(line.substring(6).trim())));
        }
        else if (line.startsWith(":")) {
          comment = (comment != null ? comment : new StringBuilder());
          comment.append(line.substring(1).trim()).append('\n');
        }
      }
    }

    Object decodedData = (data != null ? decodeData(data, valueType, hints) : null);

    if (shouldWrap) {
      if (comment != null) {
        sseBuilder.comment(comment.substring(0, comment.length() - 1));
      }
      if (decodedData != null) {
        sseBuilder.data(decodedData);
      }
      return sseBuilder.build();
    }
    else {
      return decodedData;
    }
  }

  @Nullable
  private Object decodeData(StringBuilder data, ResolvableType dataType, Map<String, Object> hints) {
    if (String.class == dataType.resolve()) {
      return data.substring(0, data.length() - 1);
    }
    if (this.decoder == null) {
      throw new CodecException("No SSE decoder configured and the data is not String.");
    }
    byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);  // wrapping only, no allocation
    return this.decoder.decode(buffer, dataType, MediaType.TEXT_EVENT_STREAM, hints);
  }

  @Override
  public Mono<Object> readMono(
          ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {

    // In order of readers, we're ahead of String + "*/*"
    // If this is called, simply delegate to StringDecoder

    if (elementType.resolve() == String.class) {
      Flux<DataBuffer> body = message.getBody();
      return this.lineDecoder.decodeToMono(body, elementType, null, null).cast(Object.class);
    }

    return Mono.error(new UnsupportedOperationException(
            "ServerSentEventHttpMessageReader only supports reading stream of events as a Flux"));
  }

  private class LimitTracker {

    private int accumulated = 0;

    public void afterLineParsed(String line) {
      if (getMaxInMemorySize() < 0) {
        return;
      }
      if (line.isEmpty()) {
        this.accumulated = 0;
      }
      if (line.length() > Integer.MAX_VALUE - this.accumulated) {
        raiseLimitException();
      }
      else {
        this.accumulated += line.length();
        if (this.accumulated > getMaxInMemorySize()) {
          raiseLimitException();
        }
      }
    }

    private void raiseLimitException() {
      // Do not release here, it's likely down via doOnDiscard..
      throw new DataBufferLimitException("Exceeded limit on max bytes to buffer : " + getMaxInMemorySize());
    }
  }

}
