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
package cn.taketoday.jdbc.type;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeReference;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 * @author TODAY
 */
@SuppressWarnings("rawtypes")
public class TypeHandlerRegistry {
  private static final TypeHandlerRegistry sharedInstance = new TypeHandlerRegistry();

  private ObjectTypeHandler objectTypeHandler = ObjectTypeHandler.getSharedInstance();

  private final TypeHandler<Object> unknownTypeHandler;
  private final HashMap<Class<?>, TypeHandler<?>> typeHandlers = new HashMap<>();
  private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumTypeHandler.class;
  private TypeHandlerResolver typeHandlerResolver = TypeHandlerResolver.forMappedTypeHandlerAnnotation();

  public TypeHandlerRegistry() {
    this.unknownTypeHandler = new UnknownTypeHandler(this);

    register(Boolean.class, new BooleanTypeHandler());
    register(boolean.class, new BooleanTypeHandler());

    register(Byte.class, new ByteTypeHandler());
    register(byte.class, new ByteTypeHandler());

    register(Short.class, new ShortTypeHandler());
    register(short.class, new ShortTypeHandler());

    register(int.class, new IntegerTypeHandler());
    register(Integer.class, new IntegerTypeHandler());

    register(Long.class, new LongTypeHandler());
    register(long.class, new LongTypeHandler());

    register(Float.class, new FloatTypeHandler());
    register(float.class, new FloatTypeHandler());

    register(Double.class, new DoubleTypeHandler());
    register(double.class, new DoubleTypeHandler());

    register(String.class, new StringTypeHandler());
    register(Reader.class, new ClobReaderTypeHandler());

    register(BigInteger.class, new BigIntegerTypeHandler());
    register(BigDecimal.class, new BigDecimalTypeHandler());

    register(byte[].class, new ByteArrayTypeHandler());
    register(Byte[].class, new ByteObjectArrayTypeHandler());
    register(InputStream.class, new BlobInputStreamTypeHandler());

    register(Object.class, unknownTypeHandler);

    register(Date.class, new DateTypeHandler());

    register(java.sql.Date.class, new SqlDateTypeHandler());
    register(java.sql.Time.class, new SqlTimeTypeHandler());
    register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());

    register(Instant.class, new InstantTypeHandler());
    register(LocalDate.class, new LocalDateTypeHandler());
    register(LocalTime.class, new LocalTimeTypeHandler());
    register(LocalDateTime.class, new LocalDateTimeTypeHandler());

    register(Year.class, new YearTypeHandler());
    register(Month.class, new MonthTypeHandler());
    register(YearMonth.class, new YearMonthTypeHandler());
    register(OffsetTime.class, new OffsetTimeTypeHandler());
    register(ZonedDateTime.class, new ZonedDateTimeTypeHandler());
    register(OffsetDateTime.class, new OffsetDateTimeTypeHandler());

    register(char.class, new CharacterTypeHandler());
    register(Character.class, new CharacterTypeHandler());

    register(UUID.class, new UUIDTypeHandler());

    registerJodaTime();
  }

  /**
   * Set a default {@link TypeHandler} class for {@link Enum}.
   * A default {@link TypeHandler} is {@link EnumTypeHandler}.
   *
   * @param typeHandler a type handler class for {@link Enum}
   */
  public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
    this.defaultEnumTypeHandler = typeHandler;
  }

  /**
   * Set a default object {@link TypeHandler}
   */
  public void setObjectTypeHandler(ObjectTypeHandler objectTypeHandler) {
    this.objectTypeHandler = objectTypeHandler;
  }

  public ObjectTypeHandler getObjectTypeHandler() {
    return objectTypeHandler;
  }

  //

  public boolean hasTypeHandler(Class<?> javaType) {
    return javaType != null && getTypeHandler(javaType) != null;
  }

  public boolean hasTypeHandler(TypeReference<?> javaTypeReference) {
    return javaTypeReference != null && getTypeHandler(javaTypeReference) != null;
  }

  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference) {
    ResolvableType resolvableType = javaTypeReference.getResolvableType();
    Class<T> aClass = (Class<T>) resolvableType.toClass();
    return getTypeHandler(aClass);
  }

  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
    TypeHandler<?> typeHandler = typeHandlers.get(type);
    if (typeHandler == null) {
      if (Enum.class.isAssignableFrom(type)) {
        typeHandler = getInstance(type, defaultEnumTypeHandler);
        register(type, typeHandler);
      }
      else {
        typeHandler = typeHandlerNotFound(type);
      }
    }
    return (TypeHandler<T>) typeHandler;
  }

  /**
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getTypeHandler(BeanProperty beanProperty) {
    TypeHandler<?> typeHandler = typeHandlerResolver.resolve(beanProperty);
    if (typeHandler != null) {
      return (TypeHandler<T>) typeHandler;
    }
    // fallback to default
    Class<?> type = beanProperty.getType();
    return (TypeHandler<T>) getTypeHandler(type);
  }

  public void addPropertyTypeHandlerResolver(TypeHandlerResolver resolver) {
    Assert.notNull(resolver, "TypeHandlerResolver is required");
    this.typeHandlerResolver = typeHandlerResolver.and(resolver);
  }

  public void addPropertyTypeHandlerResolvers(@Nullable TypeHandlerResolver... resolvers) {
    Assert.notNull(resolvers, "TypeHandlerResolver is required");
    this.typeHandlerResolver = typeHandlerResolver.and(TypeHandlerResolver.composite(resolvers));
  }

  public void setPropertyTypeHandlerResolvers(@Nullable TypeHandlerResolver... resolvers) {
    this.typeHandlerResolver = TypeHandlerResolver.composite(resolvers);
  }

  public void setPropertyTypeHandlerResolver(@Nullable TypeHandlerResolver resolver) {
    this.typeHandlerResolver = resolver == null ? TypeHandlerResolver.forMappedTypeHandlerAnnotation() : resolver;
  }

  public TypeHandlerResolver getTypeHandlerResolver() {
    return typeHandlerResolver;
  }

  protected TypeHandler<?> typeHandlerNotFound(Type type) {
    return unknownTypeHandler;
  }

  public TypeHandler<Object> getUnknownTypeHandler() {
    return unknownTypeHandler;
  }

  @SuppressWarnings("unchecked")
  public <T> void register(TypeHandler<T> typeHandler) {
    boolean mappedTypeFound = false;
    var mappedTypes = MergedAnnotations.from(typeHandler.getClass()).get(MappedTypes.class);
    if (mappedTypes.isPresent()) {
      for (Class<?> handledType : mappedTypes.getClassValueArray()) {
        register((Class<T>) handledType, typeHandler);
        mappedTypeFound = true;
      }
    }
    // try to auto-discover the mapped type
    if (!mappedTypeFound && typeHandler instanceof TypeReference typeReference) {
      try {
        register(typeReference, typeHandler);
        mappedTypeFound = true;
      }
      catch (Throwable t) {
        // maybe users define the TypeReference with a different type and are not assignable, so just ignore it
      }
    }
    if (!mappedTypeFound) {
      register((Class<T>) null, typeHandler);
    }
  }

  public <T> void register(Class<T> javaType, TypeHandler<?> typeHandler) {
    typeHandlers.put(javaType, typeHandler);
  }

  public <T> void register(TypeReference<T> reference, TypeHandler<T> handler) {
    ResolvableType resolvableType = reference.getResolvableType();
    Class<?> aClass = resolvableType.toClass();
    register(aClass, handler);
  }

  //
  // REGISTER CLASS
  //

  // Only handler type

  public void register(Class<?> typeHandlerClass) {
    boolean mappedTypeFound = false;
    var mappedTypes = MergedAnnotations.from(typeHandlerClass).get(MappedTypes.class);
    if (mappedTypes.isPresent()) {
      for (Class<?> javaTypeClass : mappedTypes.getClassValueArray()) {
        register(javaTypeClass, typeHandlerClass);
        mappedTypeFound = true;
      }
    }
    if (!mappedTypeFound) {
      register(getInstance(null, typeHandlerClass));
    }
  }

  public void register(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
    register(javaTypeClass, getInstance(javaTypeClass, typeHandlerClass));
  }

  protected void registerJodaTime() {
    try {
      register(JodaDateTimeTypeHandler.class);
      register(JodaLocalDateTypeHandler.class);
      register(JodaLocalTimeTypeHandler.class);
    }
    catch (Exception ignored) { }
  }

  // Construct a handler (used also from Builders)

  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getInstance(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
    if (javaTypeClass != null) {
      try {
        Constructor<?> c = typeHandlerClass.getConstructor(Class.class);
        return (TypeHandler<T>) c.newInstance(javaTypeClass);
      }
      catch (NoSuchMethodException ignored) {
        // ignored
      }
      catch (Exception e) {
        throw new TypeException("Failed invoking constructor for handler " + typeHandlerClass, e);
      }
    }
    try {
      Constructor<?> c = typeHandlerClass.getConstructor();
      return (TypeHandler<T>) c.newInstance();
    }
    catch (Exception e) {
      throw new TypeException("Unable to find a usable constructor for " + typeHandlerClass, e);
    }
  }

  // get information

  /**
   * Gets the type handlers.
   *
   * @return the type handlers
   */
  public Map<Class<?>, TypeHandler<?>> getTypeHandlers() {
    return typeHandlers;
  }

  public static TypeHandlerRegistry getSharedInstance() {
    return sharedInstance;
  }
}
