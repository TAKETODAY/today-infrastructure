/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.codec.json;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.http.HttpLogging;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.ObjectUtils;

/**
 * Base class providing support methods for Jackson 2.9 encoding and decoding.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class Jackson2CodecSupport {

  /**
   * The key for the hint to specify a "JSON View" for encoding or decoding
   * with the value expected to be a {@link Class}.
   *
   * @see <a href="https://www.baeldung.com/jackson-json-view-annotation">Jackson JSON Views</a>
   */
  public static final String JSON_VIEW_HINT = Jackson2CodecSupport.class.getName() + ".jsonView";

  /**
   * The key for the hint to access the actual ResolvableType passed into
   * {@link cn.taketoday.http.codec.HttpMessageReader#read(ResolvableType, ResolvableType, ServerHttpRequest, ServerHttpResponse, Map)}
   * (server-side only). Currently set when the method argument has generics because
   * in case of reactive types, use of {@code ResolvableType.getGeneric()} means no
   * MethodParameter source and no knowledge of the containing class.
   */
  static final String ACTUAL_TYPE_HINT = Jackson2CodecSupport.class.getName() + ".actualType";

  private static final String JSON_VIEW_HINT_ERROR =
          "@JsonView only supported for write hints with exactly 1 class argument: ";

  private static final List<MimeType> defaultMimeTypes = List.of(
          MediaType.APPLICATION_JSON,
          new MediaType("application", "*+json"),
          MediaType.APPLICATION_NDJSON
  );

  private static final List<MimeType> problemDetailMimeTypes =
          Collections.singletonList(MediaType.APPLICATION_PROBLEM_JSON);

  protected final Logger logger = HttpLogging.forLogName(getClass());

  private ObjectMapper defaultObjectMapper;

  @Nullable
  private Map<Class<?>, Map<MimeType, ObjectMapper>> objectMapperRegistrations;

  private final List<MimeType> mimeTypes;

  /**
   * Constructor with a Jackson {@link ObjectMapper} to use.
   */
  protected Jackson2CodecSupport(ObjectMapper objectMapper, MimeType... mimeTypes) {
    Assert.notNull(objectMapper, "ObjectMapper must not be null");
    this.defaultObjectMapper = objectMapper;
    this.mimeTypes = ObjectUtils.isNotEmpty(mimeTypes) ? List.of(mimeTypes) : defaultMimeTypes;
  }

  /**
   * Configure the default ObjectMapper instance to use.
   *
   * @param objectMapper the ObjectMapper instance
   */
  public void setObjectMapper(ObjectMapper objectMapper) {
    Assert.notNull(objectMapper, "ObjectMapper must not be null");
    this.defaultObjectMapper = objectMapper;
  }

  /**
   * Return the {@link #setObjectMapper configured} default ObjectMapper.
   */
  public ObjectMapper getObjectMapper() {
    return this.defaultObjectMapper;
  }

  /**
   * Configure the {@link ObjectMapper} instances to use for the given
   * {@link Class}. This is useful when you want to deviate from the
   * {@link #getObjectMapper() default} ObjectMapper or have the
   * {@code ObjectMapper} vary by {@code MediaType}.
   * <p><strong>Note:</strong> Use of this method effectively turns off use of
   * the default {@link #getObjectMapper() ObjectMapper} and supported
   * {@link #getMimeTypes() MimeTypes} for the given class. Therefore it is
   * important for the mappings configured here to
   * {@link MediaType#includes(MediaType) include} every MediaType that must
   * be supported for the given class.
   *
   * @param clazz the type of Object to register ObjectMapper instances for
   * @param registrar a consumer to populate or otherwise update the
   * MediaType-to-ObjectMapper associations for the given Class
   */
  public void registerObjectMappersForType(Class<?> clazz, Consumer<Map<MimeType, ObjectMapper>> registrar) {
    if (this.objectMapperRegistrations == null) {
      this.objectMapperRegistrations = new LinkedHashMap<>();
    }
    Map<MimeType, ObjectMapper> registrations =
            this.objectMapperRegistrations.computeIfAbsent(clazz, c -> new LinkedHashMap<>());
    registrar.accept(registrations);
  }

  /**
   * Return ObjectMapper registrations for the given class, if any.
   *
   * @param clazz the class to look up for registrations for
   * @return a map with registered MediaType-to-ObjectMapper registrations,
   * or empty if in case of no registrations for the given class.
   */
  @Nullable
  public Map<MimeType, ObjectMapper> getObjectMappersForType(Class<?> clazz) {
    for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
      if (entry.getKey().isAssignableFrom(clazz)) {
        return entry.getValue();
      }
    }
    return Collections.emptyMap();
  }

  protected Map<Class<?>, Map<MimeType, ObjectMapper>> getObjectMapperRegistrations() {
    return (this.objectMapperRegistrations != null ? this.objectMapperRegistrations : Collections.emptyMap());
  }

  /**
   * Subclasses should expose this as "decodable" or "encodable" mime types.
   */
  protected List<MimeType> getMimeTypes() {
    return this.mimeTypes;
  }

  protected List<MimeType> getMimeTypes(ResolvableType elementType) {
    Class<?> elementClass = elementType.toClass();
    List<MimeType> result = null;
    for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
      if (entry.getKey().isAssignableFrom(elementClass)) {
        result = result != null ? result : new ArrayList<>(entry.getValue().size());
        result.addAll(entry.getValue().keySet());
      }
    }
    if (CollectionUtils.isNotEmpty(result)) {
      return result;
    }
    return ProblemDetail.class.isAssignableFrom(elementClass)
           ? problemDetailMimeTypes
           : getMimeTypes();
  }

  protected boolean notSupportsMimeType(@Nullable MimeType mimeType) {
    if (mimeType == null) {
      return false;
    }
    for (MimeType supportedMimeType : this.mimeTypes) {
      if (supportedMimeType.isCompatibleWith(mimeType)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Determine whether to log the given exception coming from a
   * {@link ObjectMapper#canDeserialize} / {@link ObjectMapper#canSerialize} check.
   *
   * @param type the class that Jackson tested for (de-)serializability
   * @param cause the Jackson-thrown exception to evaluate
   * (typically a {@link JsonMappingException})
   */
  protected void logWarningIfNecessary(Type type, @Nullable Throwable cause) {
    if (cause == null) {
      return;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Failed to evaluate Jackson {}serialization for type [{}]", (type instanceof JavaType ? "de" : ""), type, cause);
    }
  }

  protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
    return this.defaultObjectMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
  }

  protected Map<String, Object> getHints(ResolvableType resolvableType) {
    MethodParameter param = getParameter(resolvableType);
    if (param != null) {
      HashMap<String, Object> hints = null;
      if (resolvableType.hasGenerics()) {
        hints = new HashMap<>(2);
        hints.put(ACTUAL_TYPE_HINT, resolvableType);
      }
      JsonView annotation = getAnnotation(param, JsonView.class);
      if (annotation != null) {
        Class<?>[] classes = annotation.value();
        if (classes.length != 1) {
          throw new IllegalArgumentException(JSON_VIEW_HINT_ERROR + param);
        }
        hints = hints != null ? hints : new HashMap<>(1);
        hints.put(JSON_VIEW_HINT, classes[0]);
      }
      if (hints != null) {
        return hints;
      }
    }
    return Hints.none();
  }

  @Nullable
  protected MethodParameter getParameter(ResolvableType type) {
    Object source = type.getSource();
    if (source instanceof Parameter parameter) {
      return MethodParameter.forParameter(parameter);
    }
    else if (source instanceof MethodParameter methodParameter) {
      return methodParameter;
    }
    return null;
  }

  @Nullable
  protected abstract <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType);

  /**
   * Select an ObjectMapper to use, either the main ObjectMapper or another
   * if the handling for the given Class has been customized through
   * {@link #registerObjectMappersForType(Class, Consumer)}.
   */
  @Nullable
  protected ObjectMapper selectObjectMapper(ResolvableType targetType, @Nullable MimeType targetMimeType) {
    if (targetMimeType == null || CollectionUtils.isEmpty(this.objectMapperRegistrations)) {
      return this.defaultObjectMapper;
    }
    Class<?> targetClass = targetType.toClass();
    for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> typeEntry : getObjectMapperRegistrations().entrySet()) {
      if (typeEntry.getKey().isAssignableFrom(targetClass)) {
        for (Map.Entry<MimeType, ObjectMapper> objectMapperEntry : typeEntry.getValue().entrySet()) {
          if (objectMapperEntry.getKey().includes(targetMimeType)) {
            return objectMapperEntry.getValue();
          }
        }
        // No matching registrations
        return null;
      }
    }
    // No registrations
    return this.defaultObjectMapper;
  }

}
