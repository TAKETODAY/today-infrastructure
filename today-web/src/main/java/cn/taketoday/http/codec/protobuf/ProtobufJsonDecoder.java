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

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A {@code Decoder} that reads a JSON byte stream and converts it to
 * <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>
 * {@link com.google.protobuf.Message}s.
 *
 * <p>Flux deserialized via
 * {@link #decode(Publisher, ResolvableType, MimeType, Map)} are not supported because
 * the Protobuf Java Util library does not provide a non-blocking parser
 * that splits a JSON stream into tokens.
 * Applications should consider decoding to {@code Mono<Message>} or
 * {@code Mono<List<Message>>}, which will use the supported
 * {@link #decodeToMono(Publisher, ResolvableType, MimeType, Map)}.
 *
 * <p>To generate {@code Message} Java classes, you need to install the
 * {@code protoc} binary.
 *
 * <p>This decoder requires Protobuf 3.29 or higher, and supports
 * {@code "application/json"} and {@code "application/*+json"} with
 * the official {@code "com.google.protobuf:protobuf-java-util"} library.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ProtobufJsonEncoder
 * @since 5.0
 */
public class ProtobufJsonDecoder implements Decoder<Message> {

  /** The default max size for aggregating messages. */
  protected static final int DEFAULT_MESSAGE_MAX_SIZE = 256 * 1024;

  private static final List<MimeType> defaultMimeTypes = List.of(MediaType.APPLICATION_JSON,
          new MediaType("application", "*+json"));

  private static final ConcurrentMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

  private final JsonFormat.Parser parser;

  private int maxMessageSize = DEFAULT_MESSAGE_MAX_SIZE;

  /**
   * Construct a new {@link ProtobufJsonDecoder} using a default {@link JsonFormat.Parser} instance.
   */
  public ProtobufJsonDecoder() {
    this(JsonFormat.parser());
  }

  /**
   * Construct a new {@link ProtobufJsonDecoder} using the given {@link JsonFormat.Parser} instance.
   */
  public ProtobufJsonDecoder(JsonFormat.Parser parser) {
    this.parser = parser;
  }

  /**
   * Return the {@link #setMaxMessageSize configured} message size limit.
   */
  public int getMaxMessageSize() {
    return this.maxMessageSize;
  }

  /**
   * The max size allowed per message.
   * <p>By default, this is set to 256K.
   *
   * @param maxMessageSize the max size per message, or -1 for unlimited
   */
  public void setMaxMessageSize(int maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
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
  public List<MimeType> getDecodableMimeTypes() {
    return defaultMimeTypes;
  }

  @Override
  public Flux<Message> decode(Publisher<DataBuffer> inputStream, ResolvableType targetType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    return Flux.error(new UnsupportedOperationException("Protobuf decoder does not support Flux, use Mono<List<...>> instead."));
  }

  @Override
  public Message decode(DataBuffer dataBuffer, ResolvableType targetType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {
    try {
      Message.Builder builder = getMessageBuilder(targetType.toClass());
      this.parser.merge(new InputStreamReader(dataBuffer.asInputStream()), builder);
      return builder.build();
    }
    catch (Exception ex) {
      throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
    }
    finally {
      DataBufferUtils.release(dataBuffer);
    }
  }

  /**
   * Create a new {@code Message.Builder} instance for the given class.
   * <p>This method uses a ConcurrentHashMap for caching method lookups.
   */
  private static Message.Builder getMessageBuilder(Class<?> clazz) throws Exception {
    Method method = methodCache.get(clazz);
    if (method == null) {
      method = clazz.getMethod("newBuilder");
      methodCache.put(clazz, method);
    }
    return (Message.Builder) method.invoke(clazz);
  }

  @Override
  public Mono<Message> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    return DataBufferUtils.join(inputStream, this.maxMessageSize)
            .map(dataBuffer -> decode(dataBuffer, elementType, mimeType, hints))
            .onErrorMap(DataBufferLimitException.class, exc -> new DecodingException("Could not decode JSON as Protobuf message", exc));
  }

}
