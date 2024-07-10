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

package cn.taketoday.http.codec.protobuf;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.HttpMessageEncoder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.FastByteArrayOutputStream;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A {@code Encoder} that writes {@link com.google.protobuf.Message}s as JSON.
 *
 * <p>To generate {@code Message} Java classes, you need to install the
 * {@code protoc} binary.
 *
 * <p>This encoder requires Protobuf 3.29 or higher, and supports
 * {@code "application/json"} and {@code "application/*+json"} with
 * the official {@code "com.google.protobuf:protobuf-java-util"} library.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ProtobufJsonDecoder
 * @since 5.0
 */
public class ProtobufJsonEncoder implements HttpMessageEncoder<Message> {

  private static final byte[] EMPTY_BYTES = new byte[0];

  private static final ResolvableType MESSAGE_TYPE = ResolvableType.forClass(Message.class);

  private static final List<MimeType> defaultMimeTypes = List.of(
          MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));

  private final JsonFormat.Printer printer;

  /**
   * Construct a new {@link ProtobufJsonEncoder} using a default {@link JsonFormat.Printer} instance.
   */
  public ProtobufJsonEncoder() {
    this(JsonFormat.printer());
  }

  /**
   * Construct a new {@link ProtobufJsonEncoder} using the given {@link JsonFormat.Printer} instance.
   */
  public ProtobufJsonEncoder(JsonFormat.Printer printer) {
    this.printer = printer;
  }

  @Override
  public List<MediaType> getStreamingMediaTypes() {
    return List.of(MediaType.APPLICATION_NDJSON);
  }

  @Override
  public List<MimeType> getEncodableMimeTypes() {
    return defaultMimeTypes;
  }

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
  }

  private static boolean supportsMimeType(@Nullable MimeType mimeType) {
    if (mimeType == null) {
      return false;
    }
    for (MimeType m : defaultMimeTypes) {
      if (m.isCompatibleWith(mimeType)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Flux<DataBuffer> encode(Publisher<? extends Message> inputStream, DataBufferFactory bufferFactory,
          ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    if (inputStream instanceof Mono) {
      return Mono.from(inputStream)
              .map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hints))
              .flux();
    }
    JsonArrayJoinHelper helper = new JsonArrayJoinHelper();

    // Do not prepend JSON array prefix until first signal is known, onNext vs onError
    // Keeps response not committed for error handling
    return Flux.from(inputStream)
            .map(value -> {
              byte[] prefix = helper.getPrefix();
              byte[] delimiter = helper.getDelimiter();
              DataBuffer dataBuffer = encodeValue(value, bufferFactory, MESSAGE_TYPE, mimeType, hints);
              return (prefix.length > 0 ?
                      bufferFactory.join(List.of(bufferFactory.wrap(prefix), bufferFactory.wrap(delimiter), dataBuffer)) :
                      bufferFactory.join(List.of(bufferFactory.wrap(delimiter), dataBuffer)));
            })
            .switchIfEmpty(Mono.fromCallable(() -> bufferFactory.wrap(helper.getPrefix())))
            .concatWith(Mono.fromCallable(() -> bufferFactory.wrap(helper.getSuffix())));
  }

  @Override
  public DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory, ResolvableType valueType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
    try {
      this.printer.appendTo(message, writer);
      writer.flush();
      byte[] bytes = bos.toByteArrayUnsafe();
      return bufferFactory.wrap(bytes);
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unexpected I/O error while writing to data buffer", ex);
    }
  }

  private static class JsonArrayJoinHelper {

    private static final byte[] COMMA_SEPARATOR = { ',' };

    private static final byte[] OPEN_BRACKET = { '[' };

    private static final byte[] CLOSE_BRACKET = { ']' };

    private boolean firstItemEmitted;

    public byte[] getDelimiter() {
      if (this.firstItemEmitted) {
        return COMMA_SEPARATOR;
      }
      this.firstItemEmitted = true;
      return EMPTY_BYTES;
    }

    public byte[] getPrefix() {
      return (this.firstItemEmitted ? EMPTY_BYTES : OPEN_BRACKET);
    }

    public byte[] getSuffix() {
      return CLOSE_BRACKET;
    }
  }
}
