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

package cn.taketoday.http.converter.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.converter.AbstractGenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializationException;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.json.Json;

/**
 * Implementation of {@link cn.taketoday.http.converter.HttpMessageConverter}
 * that can read and write JSON using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This converter can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports {@code application/json} and {@code application/*+json} with
 * various character sets, {@code UTF-8} being the default.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.0
 */
public class KotlinSerializationJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final Map<Type, KSerializer<Object>> serializerCache = new ConcurrentReferenceHashMap<>();

  private final Json json;

  /**
   * Construct a new {@code KotlinSerializationJsonHttpMessageConverter} with the default configuration.
   */
  public KotlinSerializationJsonHttpMessageConverter() {
    this(Json.Default);
  }

  /**
   * Construct a new {@code KotlinSerializationJsonHttpMessageConverter} with a custom configuration.
   */
  public KotlinSerializationJsonHttpMessageConverter(Json json) {
    super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    this.json = json;
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    try {
      serializer(clazz);
      return true;
    }
    catch (Exception ex) {
      return false;
    }
  }

  @Override
  public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
    try {
      serializer(GenericTypeResolver.resolveType(type, contextClass));
      return canRead(mediaType);
    }
    catch (Exception ex) {
      return false;
    }
  }

  @Override
  public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
    try {
      serializer(type != null ? GenericTypeResolver.resolveType(type, clazz) : clazz);
      return canWrite(mediaType);
    }
    catch (Exception ex) {
      return false;
    }
  }

  @Override
  public final Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    return decode(serializer(GenericTypeResolver.resolveType(type, contextClass)), inputMessage);
  }

  @Override
  protected final Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    return decode(serializer(clazz), inputMessage);
  }

  private Object decode(KSerializer<Object> serializer, HttpInputMessage inputMessage)
          throws IOException, HttpMessageNotReadableException {

    MediaType contentType = inputMessage.getHeaders().getContentType();
    String jsonText = StreamUtils.copyToString(inputMessage.getBody(), getCharsetToUse(contentType));
    try {
      // TODO Use stream based API when available
      return this.json.decodeFromString(serializer, jsonText);
    }
    catch (SerializationException ex) {
      throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex, inputMessage);
    }
  }

  @Override
  protected final void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {

    encode(object, serializer(type != null ? type : object.getClass()), outputMessage);
  }

  private void encode(Object object, KSerializer<Object> serializer, HttpOutputMessage outputMessage)
          throws IOException, HttpMessageNotWritableException {

    try {
      String json = this.json.encodeToString(serializer, object);
      MediaType contentType = outputMessage.getHeaders().getContentType();
      outputMessage.getBody().write(json.getBytes(getCharsetToUse(contentType)));
      outputMessage.getBody().flush();
    }
    catch (IOException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
    }
  }

  private Charset getCharsetToUse(@Nullable MediaType contentType) {
    if (contentType != null && contentType.getCharset() != null) {
      return contentType.getCharset();
    }
    return DEFAULT_CHARSET;
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
