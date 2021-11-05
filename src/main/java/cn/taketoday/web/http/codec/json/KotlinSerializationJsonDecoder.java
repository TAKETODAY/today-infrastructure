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

package cn.taketoday.web.http.codec.json;

import org.reactivestreams.Publisher;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.AbstractDecoder;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.MimeType;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.json.Json;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Decode a byte stream into JSON and convert to Object's with
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This decoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports {@code application/json} and {@code application/*+json} with
 * various character sets, {@code UTF-8} being the default.
 *
 * <p>Decoding streams is not supported yet, see
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * related issue.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class KotlinSerializationJsonDecoder extends AbstractDecoder<Object> {

  private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

  private final Json json;

  // String decoding needed for now, see https://github.com/Kotlin/kotlinx.serialization/issues/204 for more details
  private final StringDecoder stringDecoder = StringDecoder.allMimeTypes(StringDecoder.DEFAULT_DELIMITERS, false);

  public KotlinSerializationJsonDecoder() {
    this(Json.Default);
  }

  public KotlinSerializationJsonDecoder(Json json) {
    super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    this.json = json;
  }

  /**
   * Configure a limit on the number of bytes that can be buffered whenever
   * the input stream needs to be aggregated. This can be a result of
   * decoding to a single {@code DataBuffer},
   * {@link java.nio.ByteBuffer ByteBuffer}, {@code byte[]},
   * {@link cn.taketoday.core.io.Resource Resource}, {@code String}, etc.
   * It can also occur when splitting the input stream, e.g. delimited text,
   * in which case the limit applies to data buffered between delimiters.
   * <p>By default this is set to 256K.
   *
   * @param byteCount the max number of bytes to buffer, or -1 for unlimited
   */
  public void setMaxInMemorySize(int byteCount) {
    this.stringDecoder.setMaxInMemorySize(byteCount);
  }

  /**
   * Return the {@link #setMaxInMemorySize configured} byte count limit.
   */
  public int getMaxInMemorySize() {
    return this.stringDecoder.getMaxInMemorySize();
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    try {
      serializer(elementType.getType());
      return (super.canDecode(elementType, mimeType) && !CharSequence.class.isAssignableFrom(elementType.toClass()));
    }
    catch (Exception ex) {
      return false;
    }
  }

  @Override
  public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
                             @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return Flux.error(new UnsupportedOperationException());
  }

  @Override
  public Mono<Object> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
                                   @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return this.stringDecoder
            .decodeToMono(inputStream, elementType, mimeType, hints)
            .map(jsonText -> this.json.decodeFromString(serializer(elementType.getType()), jsonText));
  }

  /**
   * Tries to find a serializer that can marshall or unmarshall instances of the given type
   * using kotlinx.serialization. If no serializer can be found, an exception is thrown.
   * <p>Resolved serializers are cached and cached results are returned on successive calls.
   * TODO Avoid relying on throwing exception when https://github.com/Kotlin/kotlinx.serialization/pull/1164 is fixed
   *
   * @param type the type to find a serializer for
   * @return a resolved serializer for the given type
   * @throws RuntimeException if no serializer supporting the given type can be found
   */
  private KSerializer<Object> serializer(Type type) {
    KSerializer<Object> serializer = serializerCache.get(type);
    if (serializer == null) {
      serializer = SerializersKt.serializer(type);
      if (hasPolymorphism(serializer.getDescriptor(), new HashSet<>())) {
        throw new UnsupportedOperationException("Open polymorphic serialization is not supported yet");
      }
      serializerCache.put(type, serializer);
    }
    return serializer;
  }

  private boolean hasPolymorphism(SerialDescriptor descriptor, Set<String> alreadyProcessed) {
    alreadyProcessed.add(descriptor.getSerialName());
    if (descriptor.getKind().equals(PolymorphicKind.OPEN.INSTANCE)) {
      return true;
    }
    for (int i = 0; i < descriptor.getElementsCount(); i++) {
      SerialDescriptor elementDescriptor = descriptor.getElementDescriptor(i);
      if (!alreadyProcessed.contains(elementDescriptor.getSerialName()) && hasPolymorphism(elementDescriptor, alreadyProcessed)) {
        return true;
      }
    }
    return false;
  }

}
