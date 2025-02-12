/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc.type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import infra.beans.BeanProperty;
import infra.beans.BeanUtils;
import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Assert;
import infra.lang.Enumerable;
import infra.lang.Nullable;

/**
 * {@link TypeHandler} Manager
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Enumerated
 * @see MappedTypes
 * @see MappedTypeHandler
 * @since 4.0
 */
@SuppressWarnings("rawtypes")
public class TypeHandlerManager implements TypeHandlerResolver {

  public static final TypeHandlerManager sharedInstance = new TypeHandlerManager();

  private final TypeHandler<Object> unknownTypeHandler;

  private final HashMap<Class<?>, TypeHandler<?>> typeHandlers = new HashMap<>();

  /**
   * @since 5.0
   */
  private final ArrayList<SmartTypeHandler> smartTypeHandlers = new ArrayList<>();

  private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumerationValueTypeHandler.class;

  private TypeHandlerResolver typeHandlerResolver = TypeHandlerResolver.forMappedTypeHandlerAnnotation();

  public TypeHandlerManager() {
    this.unknownTypeHandler = new UnknownTypeHandler(this);
    registerDefaults(this);
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

  public void addHandlerResolver(TypeHandlerResolver resolver) {
    Assert.notNull(resolver, "TypeHandlerResolver is required");
    this.typeHandlerResolver = typeHandlerResolver.and(resolver);
  }

  public void setHandlerResolver(@Nullable TypeHandlerResolver resolver) {
    this.typeHandlerResolver = resolver == null ? TypeHandlerResolver.forMappedTypeHandlerAnnotation() : resolver;
  }

  //

  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
    TypeHandler<?> typeHandler = typeHandlers.get(type);
    if (typeHandler == null) {
      if (Enumerable.class.isAssignableFrom(type)) {
        typeHandler = new EnumerableEnumTypeHandler(type, this);
        register(type, typeHandler);
      }
      else if (Enum.class.isAssignableFrom(type)) {
        typeHandler = getInstance(type, defaultEnumTypeHandler);
        register(type, typeHandler);
      }
      else {
        typeHandler = typeHandlerNotFound(type);
      }
    }
    return (TypeHandler<T>) typeHandler;
  }

  @Nullable
  @Override
  public TypeHandler<?> resolve(BeanProperty property) {
    return getTypeHandler(property);
  }

  /**
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getTypeHandler(BeanProperty property) {
    for (SmartTypeHandler typeHandler : smartTypeHandlers) {
      if (typeHandler.supportsProperty(property)) {
        return typeHandler;
      }
    }

    TypeHandler<?> typeHandler = typeHandlerResolver.resolve(property);
    if (typeHandler == null) {
      // fallback to default
      Class<?> type = property.getType();
      typeHandler = typeHandlers.get(type);
      if (typeHandler == null) {
        if (Enumerable.class.isAssignableFrom(type)) {
          // for Enumerable type
          typeHandler = new EnumerableEnumTypeHandler(type, this);
          register(type, typeHandler);
        }
        else if (Enum.class.isAssignableFrom(type)) {
          // BeanProperty based
          var enumerated = property.mergedAnnotations().get(Enumerated.class);
          if (!enumerated.isPresent()) {
            enumerated = MergedAnnotations.from(type).get(Enumerated.class);
          }

          if (enumerated.isPresent()) {
            EnumType enumType = enumerated.getEnum(MergedAnnotation.VALUE, EnumType.class);
            if (enumType == EnumType.ORDINAL) {
              typeHandler = new EnumOrdinalTypeHandler(type);
            }
            else {
              typeHandler = new EnumTypeHandler(type);
            }
          }
          else {
            typeHandler = getInstance(type, defaultEnumTypeHandler);
          }
        }
        else {
          typeHandler = typeHandlerNotFound(type);
        }
      }
    }

    return (TypeHandler<T>) typeHandler;
  }

  protected TypeHandler<?> typeHandlerNotFound(Type type) {
    return unknownTypeHandler;
  }

  public TypeHandler<Object> getUnknownTypeHandler() {
    return unknownTypeHandler;
  }

  @SuppressWarnings("unchecked")
  public <T> void register(TypeHandler<T> typeHandler) {
    if (typeHandler instanceof SmartTypeHandler<T> smartTypeHandler) {
      smartTypeHandlers.add(smartTypeHandler);
      return;
    }
    boolean mappedTypeFound = false;
    var mappedTypes = MergedAnnotations.from(typeHandler.getClass()).get(MappedTypes.class);
    if (mappedTypes.isPresent()) {
      for (Class<?> handledType : mappedTypes.getClassValueArray()) {
        register((Class<T>) handledType, typeHandler);
        mappedTypeFound = true;
      }
    }
    // try to auto-discover the mapped type
    if (!mappedTypeFound && typeHandler instanceof ParameterizedTypeReference typeReference) {
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

  public <T> void register(ParameterizedTypeReference<T> reference, TypeHandler<T> handler) {
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

  // Construct a handler (used also from Builders)

  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getInstance(@Nullable Class<?> javaTypeClass, Class<?> typeHandlerClass) {
    if (javaTypeClass != null) {
      Constructor<?> constructor = BeanUtils.getConstructor(typeHandlerClass);
      if (constructor == null) {
        throw new IllegalStateException("No suitable constructor in " + typeHandlerClass);
      }

      try {
        if (constructor.getParameterCount() != 0) {
          Object[] args = new Object[constructor.getParameterCount()];
          Class<?>[] parameterTypes = constructor.getParameterTypes();
          int i = 0;
          for (Class<?> parameterType : parameterTypes) {
            args[i++] = resolveArg(javaTypeClass, parameterType);
          }
          return (TypeHandler<T>) BeanUtils.newInstance(constructor, args);
        }
        else {
          return (TypeHandler<T>) BeanUtils.newInstance(constructor);
        }
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

  private Object resolveArg(Class<?> propertyType, Class<?> parameterType) {
    if (parameterType == Class.class) {
      return propertyType;
    }
    if (parameterType == TypeHandlerManager.class) {
      return this;
    }
    throw new IllegalArgumentException(
            "TypeHandler Constructor parameterType '%s' currently not supported".formatted(parameterType.getName()));
  }

  public void clear() {
    typeHandlers.clear();
    smartTypeHandlers.clear();
    setHandlerResolver(null);
  }

  // static

  public static void registerDefaults(TypeHandlerManager registry) {
    registry.register(Boolean.class, new BooleanTypeHandler());
    registry.register(boolean.class, new BooleanTypeHandler());

    registry.register(Byte.class, new ByteTypeHandler());
    registry.register(byte.class, new ByteTypeHandler());

    registry.register(Short.class, new ShortTypeHandler());
    registry.register(short.class, new ShortTypeHandler());

    registry.register(int.class, new IntegerTypeHandler());
    registry.register(Integer.class, new IntegerTypeHandler());

    registry.register(Long.class, new LongTypeHandler());
    registry.register(long.class, new LongTypeHandler());

    registry.register(Float.class, new FloatTypeHandler());
    registry.register(float.class, new FloatTypeHandler());

    registry.register(Double.class, new DoubleTypeHandler());
    registry.register(double.class, new DoubleTypeHandler());

    registry.register(String.class, new StringTypeHandler());

    registry.register(BigInteger.class, new BigIntegerTypeHandler());
    registry.register(BigDecimal.class, new BigDecimalTypeHandler());

    registry.register(byte[].class, new ByteArrayTypeHandler());

    registry.register(Object.class, registry.getUnknownTypeHandler());

    registry.register(Date.class, new DateTypeHandler());

    registry.register(java.sql.Date.class, new SqlDateTypeHandler());
    registry.register(java.sql.Time.class, new SqlTimeTypeHandler());
    registry.register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());

    registry.register(Instant.class, new InstantTypeHandler());
    registry.register(Year.class, new YearTypeHandler());
    registry.register(Month.class, new MonthTypeHandler());
    registry.register(YearMonth.class, new YearMonthTypeHandler());
    registry.register(char.class, new CharacterTypeHandler());
    registry.register(Character.class, new CharacterTypeHandler());

    registry.register(UUID.class, new UUIDTypeHandler());
    // todo Duration 优化
    registry.register(Duration.class, new DurationTypeHandler());

    registry.register(LocalDate.class, new AnyTypeHandler<>(LocalDate.class));
    registry.register(LocalTime.class, new AnyTypeHandler<>(LocalTime.class));
    registry.register(LocalDateTime.class, new AnyTypeHandler<>(LocalDateTime.class));
    registry.register(OffsetTime.class, new AnyTypeHandler<>(OffsetTime.class));
    registry.register(ZonedDateTime.class, new AnyTypeHandler<>(ZonedDateTime.class));
    registry.register(OffsetDateTime.class, new AnyTypeHandler<>(OffsetDateTime.class));
  }

}
