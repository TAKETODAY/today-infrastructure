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
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.AbstractEncoder;
import cn.taketoday.core.codec.CharSequenceEncoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.MimeType;
import cn.taketoday.web.http.codec.ServerSentEvent;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.json.Json;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This encoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports {@code application/json} and {@code application/*+json} with
 * various character sets, {@code UTF-8} being the default.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class KotlinSerializationJsonEncoder extends AbstractEncoder<Object> {

  private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

  private final Json json;

  // CharSequence encoding needed for now, see https://github.com/Kotlin/kotlinx.serialization/issues/204 for more details
  private final CharSequenceEncoder charSequenceEncoder = CharSequenceEncoder.allMimeTypes();

  public KotlinSerializationJsonEncoder() {
    this(Json.Default);
  }

  public KotlinSerializationJsonEncoder(Json json) {
    super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    this.json = json;
  }

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    try {
      serializer(elementType.getType());
      return (super.canEncode(elementType, mimeType) && !String.class.isAssignableFrom(elementType.toClass()) &&
              !ServerSentEvent.class.isAssignableFrom(elementType.toClass()));
    }
    catch (Exception ex) {
      return false;
    }
  }

  @Override
  public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
                                 ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    if (inputStream instanceof Mono) {
      return Mono.from(inputStream)
              .map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hints))
              .flux();
    }
    else {
      ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
      return Flux.from(inputStream)
              .collectList()
              .map(list -> encodeValue(list, bufferFactory, listType, mimeType, hints))
              .flux();
    }
  }

  @Override
  public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
                                ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    String json = this.json.encodeToString(serializer(valueType.getType()), value);
    return this.charSequenceEncoder.encodeValue(json, bufferFactory, valueType, mimeType, null);
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
