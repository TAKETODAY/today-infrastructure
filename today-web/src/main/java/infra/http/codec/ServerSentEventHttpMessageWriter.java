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

package infra.http.codec;

import org.reactivestreams.Publisher;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.CodecException;
import infra.core.codec.Encoder;
import infra.core.codec.Hints;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpLogging;
import infra.http.MediaType;
import infra.http.ReactiveHttpOutputMessage;
import infra.http.server.reactive.ServerHttpRequest;
import infra.http.server.reactive.ServerHttpResponse;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@code HttpMessageWriter} for {@code "text/event-stream"} responses.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ServerSentEventHttpMessageWriter implements HttpMessageWriter<Object> {

  private static final Logger logger = HttpLogging.forLogName(ServerSentEventHttpMessageWriter.class);

  private static final MediaType DEFAULT_MEDIA_TYPE = new MediaType("text", "event-stream", StandardCharsets.UTF_8);

  private static final List<MediaType> WRITABLE_MEDIA_TYPES = Collections.singletonList(MediaType.TEXT_EVENT_STREAM);

  @Nullable
  private final Encoder<?> encoder;

  /**
   * Constructor without an {@code Encoder}. In this mode only {@code String}
   * is supported for event data to be encoded.
   */
  public ServerSentEventHttpMessageWriter() {
    this(null);
  }

  /**
   * Constructor with JSON {@code Encoder} for encoding objects.
   * Support for {@code String} event data is built-in.
   *
   * @param encoder the Encoder to use (may be {@code null})
   */
  public ServerSentEventHttpMessageWriter(@Nullable Encoder<?> encoder) {
    this.encoder = encoder;
  }

  /**
   * Return the configured {@code Encoder}, if any.
   */
  @Nullable
  public Encoder<?> getEncoder() {
    return this.encoder;
  }

  @Override
  public List<MediaType> getWritableMediaTypes() {
    return WRITABLE_MEDIA_TYPES;
  }

  @Override
  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    return mediaType == null || MediaType.TEXT_EVENT_STREAM.includes(mediaType)
            || ServerSentEvent.class.isAssignableFrom(elementType.toClass());
  }

  @Override
  public Mono<Void> write(Publisher<?> input, ResolvableType elementType,
          @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

    mediaType = (mediaType != null && mediaType.getCharset() != null ? mediaType : DEFAULT_MEDIA_TYPE);
    DataBufferFactory bufferFactory = message.bufferFactory();

    message.getHeaders().setContentType(mediaType);
    return message.writeAndFlushWith(encode(input, elementType, mediaType, bufferFactory, hints));
  }

  private Flux<Publisher<DataBuffer>> encode(Publisher<?> input, ResolvableType elementType,
          MediaType mediaType, DataBufferFactory factory, Map<String, Object> hints) {

    ResolvableType dataType = (ServerSentEvent.class.isAssignableFrom(elementType.toClass()) ?
            elementType.getGeneric() : elementType);

    return Flux.from(input).map(element -> {

      ServerSentEvent<?> sse = (element instanceof ServerSentEvent<?> serverSentEvent ?
              serverSentEvent : ServerSentEvent.builder().data(element).build());

      String sseText = sse.format();
      Object data = sse.data();

      Flux<DataBuffer> result;
      if (data == null) {
        result = Flux.just(encodeText(sseText + "\n", mediaType, factory));
      }
      else if (data instanceof String text) {
        text = StringUtils.replace(text, "\n", "\ndata:");
        result = Flux.just(encodeText(sseText + text + "\n\n", mediaType, factory));
      }
      else {
        result = encodeEvent(sseText, data, dataType, mediaType, factory, hints);
      }

      return result.doOnDiscard(DataBuffer.class, DataBuffer::release);
    });
  }

  @SuppressWarnings("unchecked")
  private <T> Flux<DataBuffer> encodeEvent(CharSequence sseText, T data, ResolvableType dataType,
          MediaType mediaType, DataBufferFactory factory, Map<String, Object> hints) {

    if (this.encoder == null) {
      throw new CodecException("No SSE encoder configured and the data is not String.");
    }

    return Flux.defer(() -> {
      DataBuffer startBuffer = encodeText(sseText, mediaType, factory);
      DataBuffer endBuffer = encodeText("\n\n", mediaType, factory);
      DataBuffer dataBuffer = ((Encoder<T>) this.encoder).encodeValue(data, factory, dataType, mediaType, hints);
      Hints.touchDataBuffer(dataBuffer, hints, logger);
      return Flux.just(startBuffer, dataBuffer, endBuffer);
    });
  }

  private DataBuffer encodeText(CharSequence text, MediaType mediaType, DataBufferFactory bufferFactory) {
    Assert.notNull(mediaType.getCharset(), "Expected MediaType with charset");
    byte[] bytes = text.toString().getBytes(mediaType.getCharset());
    return bufferFactory.wrap(bytes);  // wrapping, not allocating
  }

  @Override
  public Mono<Void> write(Publisher<?> input, ResolvableType actualType, ResolvableType elementType,
          @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

    Map<String, Object> allHints = Hints.merge(hints,
            getEncodeHints(actualType, elementType, mediaType, request, response));

    return write(input, elementType, mediaType, response, allHints);
  }

  private Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType,
          @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

    if (this.encoder instanceof HttpMessageEncoder<?> httpMessageEncoder) {
      return httpMessageEncoder.getEncodeHints(actualType, elementType, mediaType, request, response);
    }
    return Hints.none();
  }

}
