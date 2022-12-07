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

package cn.taketoday.http.codec.multipart;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.ResolvableTypeProvider;
import cn.taketoday.core.codec.CharSequenceEncoder;
import cn.taketoday.core.codec.CodecException;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.codec.EncoderHttpMessageWriter;
import cn.taketoday.http.codec.FormHttpMessageWriter;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.http.codec.ResourceHttpMessageWriter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LogFormatUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link HttpMessageWriter} for writing a {@code MultiValueMap<String, ?>}
 * as multipart form data, i.e. {@code "multipart/form-data"}, to the body
 * of a request.
 *
 * <p>The serialization of individual parts is delegated to other writers.
 * By default only {@link String} and {@link Resource} parts are supported but
 * you can configure others through a constructor argument.
 *
 * <p>This writer can be configured with a {@link FormHttpMessageWriter} to
 * delegate to. It is the preferred way of supporting both form data and
 * multipart data (as opposed to registering each writer separately) so that
 * when the {@link MediaType} is not specified and generics are not present on
 * the target element type, we can inspect the values in the actual map and
 * decide whether to write plain form data (String values only) or otherwise.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see FormHttpMessageWriter
 * @since 4.0
 */
public class MultipartHttpMessageWriter
        extends MultipartWriterSupport implements HttpMessageWriter<MultiValueMap<String, ?>> {

  /** Suppress logging from individual part writers (full map logged at this level). */
  private static final Map<String, Object> DEFAULT_HINTS = Hints.from(Hints.SUPPRESS_LOGGING_HINT, true);

  private final List<HttpMessageWriter<?>> partWriters;

  @Nullable
  private final HttpMessageWriter<MultiValueMap<String, String>> formWriter;

  /**
   * Constructor with a default list of part writers (String and Resource).
   */
  public MultipartHttpMessageWriter() {
    this(Arrays.asList(
            new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()),
            new ResourceHttpMessageWriter()
    ));
  }

  /**
   * Constructor with explicit list of writers for serializing parts.
   */
  public MultipartHttpMessageWriter(List<HttpMessageWriter<?>> partWriters) {
    this(partWriters, new FormHttpMessageWriter());
  }

  /**
   * Constructor with explicit list of writers for serializing parts and a
   * writer for plain form data to fall back when no media type is specified
   * and the actual map consists of String values only.
   *
   * @param partWriters the writers for serializing parts
   * @param formWriter the fallback writer for form data, {@code null} by default
   */
  public MultipartHttpMessageWriter(
          List<HttpMessageWriter<?>> partWriters,
          @Nullable HttpMessageWriter<MultiValueMap<String, String>> formWriter) {

    super(initMediaTypes(formWriter));
    this.partWriters = partWriters;
    this.formWriter = formWriter;
  }

  private static List<MediaType> initMediaTypes(@Nullable HttpMessageWriter<?> formWriter) {
    List<MediaType> result = new ArrayList<>(MultipartHttpMessageReader.MIME_TYPES);
    if (formWriter != null) {
      result.addAll(formWriter.getWritableMediaTypes());
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Return the configured part writers.
   *
   * @since 4.0
   */
  public List<HttpMessageWriter<?>> getPartWriters() {
    return this.partWriters;
  }

  /**
   * Return the configured form writer.
   */
  @Nullable
  public HttpMessageWriter<MultiValueMap<String, String>> getFormWriter() {
    return this.formWriter;
  }

  @Override
  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    if (MultiValueMap.class.isAssignableFrom(elementType.toClass())) {
      if (mediaType == null) {
        return true;
      }
      for (MediaType supportedMediaType : getWritableMediaTypes()) {
        if (supportedMediaType.isCompatibleWith(mediaType)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Mono<Void> write(
          Publisher<? extends MultiValueMap<String, ?>> inputStream,
          ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
          Map<String, Object> hints) {

    return Mono.from(inputStream)
            .flatMap(map -> {
              if (this.formWriter == null || isMultipart(map, mediaType)) {
                return writeMultipart(map, outputMessage, mediaType, hints);
              }
              else {
                @SuppressWarnings("unchecked")
                Mono<MultiValueMap<String, String>> input = Mono.just((MultiValueMap<String, String>) map);
                return this.formWriter.write(input, elementType, mediaType, outputMessage, hints);
              }
            });
  }

  private boolean isMultipart(MultiValueMap<String, ?> map, @Nullable MediaType contentType) {
    if (contentType != null) {
      return contentType.getType().equalsIgnoreCase("multipart");
    }
    for (List<?> values : map.values()) {
      for (Object value : values) {
        if (value != null && !(value instanceof String)) {
          return true;
        }
      }
    }
    return false;
  }

  private Mono<Void> writeMultipart(
          MultiValueMap<String, ?> map,
          ReactiveHttpOutputMessage outputMessage,
          @Nullable MediaType mediaType, Map<String, Object> hints) {

    byte[] boundary = generateMultipartBoundary();

    mediaType = getMultipartMediaType(mediaType, boundary);
    outputMessage.getHeaders().setContentType(mediaType);

    if (logger.isDebugEnabled()) {
      LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Encoding " +
              (isEnableLoggingRequestDetails() ?
               LogFormatUtils.formatValue(map, !traceOn) :
               "parts " + map.keySet() + " (content masked)"));
    }
    DataBufferFactory bufferFactory = outputMessage.bufferFactory();

    Flux<DataBuffer> body = Flux.fromIterable(map.entrySet())
            .concatMap(entry -> encodePartValues(boundary, entry.getKey(), entry.getValue(), bufferFactory))
            .concatWith(generateLastLine(boundary, bufferFactory))
            .doOnDiscard(DataBuffer.class, DataBufferUtils::release);

    if (logger.isDebugEnabled()) {
      body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
    }

    return outputMessage.writeWith(body);
  }

  private Flux<DataBuffer> encodePartValues(
          byte[] boundary, String name, List<?> values, DataBufferFactory bufferFactory) {

    return Flux.fromIterable(values)
            .concatMap(value -> encodePart(boundary, name, value, bufferFactory));
  }

  @SuppressWarnings("unchecked")
  private <T> Flux<DataBuffer> encodePart(byte[] boundary, String name, T value, DataBufferFactory factory) {
    MultipartHttpOutputMessage message = new MultipartHttpOutputMessage(factory);
    HttpHeaders headers = message.getHeaders();

    Object body;
    ResolvableType resolvableType = null;
    if (value instanceof HttpEntity httpEntity) {
      headers.putAll(httpEntity.getHeaders());
      body = httpEntity.getBody();
      Assert.state(body != null, "MultipartHttpMessageWriter only supports HttpEntity with body");
      if (httpEntity instanceof ResolvableTypeProvider provider) {
        resolvableType = provider.getResolvableType();
      }
    }
    else {
      body = value;
    }
    if (resolvableType == null) {
      resolvableType = ResolvableType.fromClass(body.getClass());
    }

    if (!headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
      if (body instanceof Resource resource) {
        headers.setContentDispositionFormData(name, resource.getName());
      }
      else if (resolvableType.resolve() == Resource.class) {
        body = Mono.from((Publisher<Resource>) body)
                .doOnNext(res -> headers.setContentDispositionFormData(name, res.getName()));
      }
      else {
        headers.setContentDispositionFormData(name, null);
      }
    }

    MediaType contentType = headers.getContentType();

    final ResolvableType finalBodyType = resolvableType;
    for (HttpMessageWriter<?> partWriter : partWriters) {
      if (partWriter.canWrite(finalBodyType, contentType)) {
        // The writer will call MultipartHttpOutputMessage#write which doesn't actually write
        // but only stores the body Flux and returns Mono.empty().
        @SuppressWarnings("rawtypes")
        Publisher inputStream = body instanceof Publisher ? (Publisher) body : Mono.just(body);
        // After partContentReady, we can access the part content from MultipartHttpOutputMessage
        // and use it for writing to the actual request body
        Flux<DataBuffer> partContent = partWriter.write(inputStream,
                        resolvableType, contentType, message, DEFAULT_HINTS)
                .thenMany(Flux.defer(message::getBody));

        return Flux.concat(
                generateBoundaryLine(boundary, factory),
                partContent,
                generateNewLine(factory));
      }
    }

    return Flux.error(new CodecException("No suitable writer found for part: " + name));
  }

  private class MultipartHttpOutputMessage implements ReactiveHttpOutputMessage {

    private final DataBufferFactory bufferFactory;
    private final HttpHeaders headers = HttpHeaders.create();
    private final AtomicBoolean committed = new AtomicBoolean();

    @Nullable
    private Flux<DataBuffer> body;

    public MultipartHttpOutputMessage(DataBufferFactory bufferFactory) {
      this.bufferFactory = bufferFactory;
    }

    @Override
    public HttpHeaders getHeaders() {
      return (this.body != null ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
    }

    @Override
    public DataBufferFactory bufferFactory() {
      return this.bufferFactory;
    }

    @Override
    public void beforeCommit(Supplier<? extends Mono<Void>> action) {
      this.committed.set(true);
    }

    @Override
    public boolean isCommitted() {
      return this.committed.get();
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
      if (this.body != null) {
        return Mono.error(new IllegalStateException("Multiple calls to writeWith() not supported"));
      }
      this.body = generatePartHeaders(this.headers, this.bufferFactory).concatWith(body);
      // We don't actually want to write (just save the body Flux)
      return Mono.empty();
    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
      return Mono.error(new UnsupportedOperationException());
    }

    public Flux<DataBuffer> getBody() {
      return body != null
             ? body : Flux.error(new IllegalStateException("Body has not been written yet"));
    }

    @Override
    public Mono<Void> setComplete() {
      return Mono.error(new UnsupportedOperationException());
    }
  }

}
