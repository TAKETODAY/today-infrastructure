/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.conversion.support;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.conversion.ConfigurableConversionService;
import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.conversion.ConverterNotFoundException;
import cn.taketoday.context.conversion.ConverterRegistry;
import cn.taketoday.context.conversion.ConverterTypeConverter;
import cn.taketoday.context.conversion.StringSourceTypeConverter;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.Mappings;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.ReflectionUtils;

import static cn.taketoday.context.utils.OrderUtils.reversedSort;

/**
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDefaultConverters(ConverterRegistry)} utility method for ad-hoc
 * use against any {@code ConverterRegistry} instance.
 *
 * @author TODAY 2021/3/20 22:42
 * @since 3.0
 */
public class DefaultConversionService implements ConfigurableConversionService {

  private static final NopTypeConverter NO_MATCH = new NopTypeConverter();

  private static DefaultConversionService sharedInstance = new DefaultConversionService();

  static {
    addDefaultConverters(sharedInstance);
  }

  private final LinkedList<TypeConverter> converters = new LinkedList<>();
  private final ConverterMappings converterMappings = new ConverterMappings();
  private ConverterTypeConverter converterTypeConverter = ConverterTypeConverter.getSharedInstance();

  /**
   * Get Target {@link TypeConverter}
   *
   * @param source
   *         input source
   * @param targetClass
   *         convert to target class
   *
   * @return TypeConverter
   */
  @Override
  public TypeConverter getConverter(Object source, Class<?> targetClass) {
    final ConverterKey key = new ConverterKey(targetClass, source.getClass());
    final TypeConverter typeConverter = converterMappings.get(key, source);
    if (typeConverter != NO_MATCH) {
      return typeConverter;
    }
    return null;
  }

  class ConverterMappings extends Mappings<TypeConverter, Object> {

    @Override
    protected TypeConverter createValue(Object key, Object source) {
      final ConverterKey cacheKey = (ConverterKey) key;
      final Class<?> targetType = cacheKey.targetType;

      for (TypeConverter converter : converters) {
        if (converter.supports(targetType, source)) {
          return converter;
        }
      }

      return NO_MATCH;
    }
  }

  static class ConverterKey {
    final Class<?> targetType;
    final Class<?> sourceType;

    ConverterKey(Class<?> targetType, Class<?> sourceType) {
      this.targetType = targetType;
      this.sourceType = sourceType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ConverterKey)) return false;
      final ConverterKey that = (ConverterKey) o;
      return Objects.equals(targetType, that.targetType) && Objects.equals(sourceType, that.sourceType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(targetType, sourceType);
    }
  }

  public List<TypeConverter> getConverters() {
    return converters;
  }

  @Override
  public void setConverters(TypeConverter... converters) {
    Assert.notNull(converters, "TypeConverter must not be null");

    this.converters.clear();
    Collections.addAll(this.converters, reversedSort(converters));
  }

  @Override
  public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
    return false;
  }

  @Override
  public boolean canConvert(Object source, Class<?> targetClass) {
    return getConverter(source, targetClass) != null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T convert(Object source, Class<T> targetType) {
    if (source == null) {
      return convertNull(targetType);
    }
    Assert.notNull(targetType, "targetType must not be null");
    if (targetType.isInstance(source)) {
      return (T) source;
    }
    final TypeConverter typeConverter = getConverter(source, targetType);
    if (typeConverter == null) {
      return handleConverterNotFound(source, targetType);
    }
    return (T) typeConverter.convert(targetType, source);
  }

  protected <T> T handleConverterNotFound(Object source, Class<T> targetType) {
    throw new ConverterNotFoundException(
            "There isn't a converter to convert: ["
                    + source + "] '" + source.getClass() + "' to target class: [" + targetType + "]",
            source, targetType);
  }

  @SuppressWarnings("unchecked")
  protected <T> T convertNull(Class<T> targetClass) {
    if (targetClass == Optional.class) {
      return (T) Optional.empty();
    }
    return null;
  }

  /**
   * Add {@link TypeConverter} to {@link #converters}
   *
   * @param converters
   *         {@link TypeConverter} object
   *
   * @since 2.1.6
   */
  @Override
  public void addConverters(TypeConverter... converters) {
    if (ObjectUtils.isNotEmpty(converters)) {
      final List<TypeConverter> typeConverters = new ArrayList<>();
      Collections.addAll(typeConverters, converters);
      addConverters(typeConverters);
    }
  }

  /**
   * Add a list of {@link TypeConverter} to {@link #converters}
   *
   * @param converters
   *         {@link TypeConverter} object
   *
   * @since 2.1.6
   */
  @Override
  public void addConverters(List<TypeConverter> converters) {

    if (ObjectUtils.isNotEmpty(converters)) {
      if (getConverters() != null) {
        Collections.addAll(converters, getConverters());
      }
      setConverters(converters.toArray(new TypeConverter[converters.size()]));
    }
  }

  @Override
  public void addConverters(Converter<?, ?>... converters) {
    converterTypeConverter.addConverters(converters);
  }

  @Override
  public void addConverter(Converter<?, ?> converter) {
    converterTypeConverter.addConverter(converter);
  }

  @Override
  public void addConverter(Class<?> targetClass, Converter<?, ?> converter) {
    converterTypeConverter.addConverter(targetClass, converter);
  }

  @Override
  public <S, T>
  void addConverter(Class<T> targetClass, Class<S> sourceClass, Converter<? super S, ? extends T> converter) {
    converterTypeConverter.addConverter(targetClass, sourceClass, converter);
  }

  @Override
  public void setConverterTypeConverter(ConverterTypeConverter converterTypeConverter) {
    this.converterTypeConverter = converterTypeConverter;
  }

  @Override
  public ConverterTypeConverter getConverterTypeConverter() {
    return converterTypeConverter;
  }

  // static

  public static void setSharedInstance(DefaultConversionService sharedInstance) {
    DefaultConversionService.sharedInstance = sharedInstance;
  }

  public static DefaultConversionService getSharedInstance() {
    return sharedInstance;
  }

  /**
   * Add converters appropriate for most environments.
   *
   * @param registry
   *         the registry of converters to add to
   *         (must also be castable to ConversionService, e.g. being a {@link ConfigurableConversionService})
   *
   * @throws ClassCastException
   *         if the given ConverterRegistry could not be cast to a ConversionService
   */
  public static void addDefaultConverters(ConverterRegistry registry) {
    addScalarConverters(registry);
    addCollectionConverters(registry);

    registry.addConverters(new StringToTimeZoneConverter(),
                           new ZoneIdToTimeZoneConverter(),
                           new ZonedDateTimeToCalendarConverter());

    registry.addConverters(new ByteBufferConverter((ConversionService) registry),
                           new ObjectToObjectConverter(),
                           new IdToEntityConverter((ConversionService) registry),
                           new FallbackObjectToStringConverter(),
                           new ObjectToOptionalConverter((ConversionService) registry),

                           new PrimitiveClassConverter(),
                           registry.getConverterTypeConverter(),
                           new StringToResourceConverter(),
                           new StringSourceConstructorConverter()

    );

  }

  /**
   * Add common collection converters.
   *
   * @param registry
   *         the registry of converters to add to
   *         (must also be castable to ConversionService, e.g. being a {@link ConfigurableConversionService})
   *
   * @throws ClassCastException
   *         if the given ConverterRegistry could not be cast to a ConversionService
   * @since 4.2.3
   */
  public static void addCollectionConverters(ConverterRegistry registry) {
    ConversionService conversionService = (ConversionService) registry;

    registry.addConverters(new ArrayToCollectionConverter(conversionService));
    registry.addConverters(new CollectionToArrayConverter(conversionService));

    registry.addConverters(new ArrayToArrayConverter(conversionService));
    registry.addConverters(new CollectionToCollectionConverter(conversionService));
    registry.addConverters(new MapToMapConverter(conversionService));

    registry.addConverters(new ArrayToStringConverter(conversionService));
    registry.addConverters(new StringToArrayConverter(conversionService));

    registry.addConverters(new ArrayToObjectConverter(conversionService));
    registry.addConverters(new ObjectToArrayConverter(conversionService));

    registry.addConverters(new CollectionToStringConverter(conversionService));
    registry.addConverters(new StringToCollectionConverter(conversionService));

    registry.addConverters(new CollectionToObjectConverter(conversionService));
    registry.addConverters(new ObjectToCollectionConverter(conversionService));

    registry.addConverters(new StreamConverter(conversionService));
  }

  private static void addScalarConverters(ConverterRegistry registry) {

    registry.addConverter(String.class, Number.class, ObjectToStringConverter.INSTANCE);

    registry.addConverter(new StringToCharacterConverter());
    registry.addConverter(String.class, Character.class, ObjectToStringConverter.INSTANCE);

    registry.addConverter(new NumberToCharacterConverter());
    registry.addConverterFactory(new CharacterToNumberFactory());

    registry.addConverters(new CharacterToNumberConverter((ConversionService) registry));
    registry.addConverter(new StringToBooleanConverter());
    registry.addConverter(String.class, Boolean.class, ObjectToStringConverter.INSTANCE);

    registry.addConverters(new StringToEnumConverter());
    registry.addConverter(new EnumToStringConverter((ConversionService) registry));

    //converterRegistry.addConverterFactory(new IntegerToEnumConverterFactory());

    registry.addConverters(new IntegerToEnumConverter());
    registry.addConverter(new EnumToIntegerConverter((ConversionService) registry));

    registry.addConverter(new StringToLocaleConverter());
    registry.addConverter(String.class, Locale.class, ObjectToStringConverter.INSTANCE);

    registry.addConverter(new StringToCharsetConverter());
    registry.addConverter(String.class, Charset.class, ObjectToStringConverter.INSTANCE);

    registry.addConverter(new StringToCurrencyConverter());
    registry.addConverter(String.class, Currency.class, ObjectToStringConverter.INSTANCE);

    registry.addConverter(new StringToPropertiesConverter());
    registry.addConverter(new PropertiesToStringConverter());

    registry.addConverter(new StringToUUIDConverter());
    registry.addConverter(String.class, UUID.class, ObjectToStringConverter.INSTANCE);
  }

  // TypeConverter

  /**
   * @author TODAY <br>
   * 2019-06-06 16:12
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  static class StringSourceConstructorConverter extends StringSourceTypeConverter {

    @Override
    public boolean supportsInternal(Class<?> targetClass, Object source) {
      try {
        targetClass.getDeclaredConstructor(String.class);
        return true;
      }
      catch (NoSuchMethodException e) {
        return false;
      }
    }

    @Override
    protected Object convertInternal(Class<?> targetClass, String source) {
      try {
        return ReflectionUtils.accessibleConstructor(targetClass, String.class)
                .newInstance(source);
      }
      catch (Throwable e) {
        throw new ConversionException(e);
      }
    }
  }

  /**
   * @author TODAY <br>
   * 2019-06-19 12:28
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  static class PrimitiveClassConverter implements TypeConverter {

    @Override
    public boolean supports(Class<?> targetClass, Object source) {

      if (targetClass.isArray()) {
        targetClass = targetClass.getComponentType();
      }

      return (targetClass == boolean.class && source instanceof Boolean) //
              || (targetClass == long.class && source instanceof Long)//
              || (targetClass == int.class && source instanceof Integer)//
              || (targetClass == float.class && source instanceof Float)//
              || (targetClass == short.class && source instanceof Short)//
              || (targetClass == double.class && source instanceof Double)//
              || (targetClass == char.class && source instanceof Character)//
              || (targetClass == byte.class && source instanceof Byte);
    }

    @Override
    public Object convert(Class<?> targetClass, Object source) {
      return source; // auto convert
    }
  }

  static class NopTypeConverter implements TypeConverter {

    @Override
    public boolean supports(Class<?> targetType, Object source) {
      return false;
    }

    @Override
    public Object convert(Class<?> targetType, Object source) {
      return null;
    }
  }

}
