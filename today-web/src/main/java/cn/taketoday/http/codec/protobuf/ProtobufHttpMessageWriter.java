/*
 * Copyright 2017 - 2023 the original author or authors.
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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.codec.Encoder;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.codec.EncoderHttpMessageWriter;
import cn.taketoday.http.codec.HttpMessageEncoder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@code HttpMessageWriter} that can write a protobuf {@link Message} and adds
 * {@code X-Protobuf-Schema}, {@code X-Protobuf-Message} headers and a
 * {@code delimited=true} parameter is added to the content type if a flux is serialized.
 *
 * <p>For {@code HttpMessageReader}, just use
 * {@code new DecoderHttpMessageReader(new ProtobufDecoder())}.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ProtobufEncoder
 * @since 4.0
 */
public class ProtobufHttpMessageWriter extends EncoderHttpMessageWriter<Message> {

  private static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";
  private static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";

  private static final ConcurrentReferenceHashMap<Class<?>, Method> methodCache
          = new ConcurrentReferenceHashMap<>();

  /**
   * Create a new {@code ProtobufHttpMessageWriter} with a default {@link ProtobufEncoder}.
   */
  public ProtobufHttpMessageWriter() {
    super(new ProtobufEncoder());
  }

  /**
   * Create a new {@code ProtobufHttpMessageWriter} with the given encoder.
   *
   * @param encoder the Protobuf message encoder to use
   */
  public ProtobufHttpMessageWriter(Encoder<Message> encoder) {
    super(encoder);
  }

  @Override
  public Mono<Void> write(Publisher<? extends Message> inputStream, ResolvableType elementType,
          @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {
    try {
      HttpHeaders headers = message.getHeaders();
      Message.Builder builder = getMessageBuilder(elementType.toClass());
      Descriptors.Descriptor descriptor = builder.getDescriptorForType();
      headers.add(X_PROTOBUF_SCHEMA_HEADER, descriptor.getFile().getName());
      headers.add(X_PROTOBUF_MESSAGE_HEADER, descriptor.getFullName());
      if (inputStream instanceof Flux) {
        if (mediaType == null) {
          headers.setContentType(((HttpMessageEncoder<?>) getEncoder()).getStreamingMediaTypes().get(0));
        }
        else if (!ProtobufEncoder.DELIMITED_VALUE.equals(mediaType.getParameters().get(ProtobufEncoder.DELIMITED_KEY))) {
          Map<String, String> parameters = new HashMap<>(mediaType.getParameters());
          parameters.put(ProtobufEncoder.DELIMITED_KEY, ProtobufEncoder.DELIMITED_VALUE);
          headers.setContentType(new MediaType(mediaType.getType(), mediaType.getSubtype(), parameters));
        }
      }
      return super.write(inputStream, elementType, mediaType, message, hints);
    }
    catch (Exception ex) {
      return Mono.error(new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex));
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

}
