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

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
 * An {@code Encoder} that writes {@link com.google.protobuf.Message}s
 * using <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>.
 *
 * <p>Flux are serialized using
 * <a href="https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming">delimited Protobuf messages</a>
 * with the size of each message specified before the message itself. Single values are
 * serialized using regular Protobuf message format (without the size prepended before the message).
 *
 * <p>To generate {@code Message} Java classes, you need to install the {@code protoc} binary.
 *
 * <p>This encoder requires Protobuf 3.29 or higher, and supports
 * {@code "application/x-protobuf"} and {@code "application/octet-stream"} with the official
 * {@code "com.google.protobuf:protobuf-java"} library.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ProtobufDecoder
 * @since 4.0
 */
public class ProtobufEncoder extends ProtobufCodecSupport implements HttpMessageEncoder<Message> {

  private static final List<MediaType> streamingMediaTypes = MIME_TYPES
          .stream()
          .map(mimeType -> new MediaType(mimeType.getType(), mimeType.getSubtype(),
                  Collections.singletonMap(DELIMITED_KEY, DELIMITED_VALUE)))
          .collect(Collectors.toList());

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
  }

  @Override
  public Flux<DataBuffer> encode(Publisher<? extends Message> inputStream, DataBufferFactory bufferFactory,
          ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Flux.from(inputStream)
            .map(message -> encodeValue(message, bufferFactory, !(inputStream instanceof Mono)));
  }

  @Override
  public DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory,
          ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return encodeValue(message, bufferFactory, false);
  }

  private DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory, boolean delimited) {
    FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
    try {
      if (delimited) {
        message.writeDelimitedTo(bos);
      }
      else {
        message.writeTo(bos);
      }
      byte[] bytes = bos.toByteArrayUnsafe();
      return bufferFactory.wrap(bytes);
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unexpected I/O error while writing to data buffer", ex);
    }
  }

  @Override
  public List<MediaType> getStreamingMediaTypes() {
    return streamingMediaTypes;
  }

  @Override
  public List<MimeType> getEncodableMimeTypes() {
    return getMimeTypes();
  }

}
