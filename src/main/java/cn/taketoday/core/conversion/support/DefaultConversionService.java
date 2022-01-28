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

package cn.taketoday.core.conversion.support;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.conversion.ConfigurableConversionService;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.conversion.ConverterRegistry;
import cn.taketoday.core.conversion.TypeCapable;
import cn.taketoday.core.conversion.MatchingConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MapCache;
import cn.taketoday.util.ObjectUtils;

/**
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDefaultConverters(ConverterRegistry)} utility method for ad-hoc
 * use against any {@code ConverterRegistry} instance.
 *
 * @author TODAY 2021/3/20 22:42
 * @since 3.0
 */
@SuppressWarnings("unchecked")
public class DefaultConversionService implements ConfigurableConversionService, ArraySizeTrimmer {

  private static final NopMatchingConverter NO_MATCH = new NopMatchingConverter();

  private static final DefaultConversionService sharedInstance = new DefaultConversionService();

  static {
    addDefaultConverters(sharedInstance);
    sharedInstance.trimToSize();
  }

  private final ArrayList<MatchingConverter> converters = new ArrayList<>();
  private final ConverterMapCache converterMappings = new ConverterMapCache();
  /** @since 3.0.4 */
  private final HashMap<Class<?>, Object> nullMappings = new HashMap<>();

  public DefaultConversionService() {
    addNullValue(Optional.class, Optional.empty());
    addNullValue(boolean.class, false);
    addNullValue(int.class, 0);
    addNullValue(char.class, (char) 0);
    addNullValue(long.class, 0L);
    addNullValue(short.class, (short) 0);
    addNullValue(byte.class, (byte) 0);
    addNullValue(float.class, 0f);
    addNullValue(double.class, 0D);
  }

  @Override
  public boolean canConvert(Class<?> sourceType, TypeDescriptor targetType) {
    return getConverter(sourceType, targetType) != null;
  }

  @Override
  public <T> T convert(Object source, TypeDescriptor targetType) {
    if (source == null) {
      return convertNull(targetType);
    }
    Assert.notNull(targetType, "targetType must not be null");
    MatchingConverter matchingConverter = getConverter(source.getClass(), targetType);
    if (matchingConverter == null) {
      return handleConverterNotFound(source, targetType);
    }
    try {
      return (T) matchingConverter.convert(targetType, source);
    }
    catch (ConversionFailedException ex) {
      throw ex;
    }
    catch (Throwable ex) {
      throw new ConversionFailedException(ex, source, targetType);
    }
  }

  @SuppressWarnings("unchecked")
  protected <T> T handleConverterNotFound(
          Object source, TypeDescriptor targetType) throws ConverterNotFoundException {
    if (targetType.isInstance(source)) {
      return (T) source;
    }
    throw new ConverterNotFoundException(source, targetType);
  }

  @SuppressWarnings("unchecked")
  protected <T> T convertNull(TypeDescriptor targetType) {
    return (T) nullMappings.get(targetType.getType());
  }

  /**
   * add null value mapping
   *
   * @see #convertNull(TypeDescriptor)
   * @since 3.0.4
   */
  public <T> void addNullValue(Class<T> type, T value) {
    nullMappings.put(type, value);
  }

  /**
   * @since 3.0.4
   */
  public void removeNullValue(Class<?> type) {
    nullMappings.remove(type);
  }

  /**
   * Get Target {@link MatchingConverter}
   *
   * @param sourceType input sourceType
   * @param targetType convert to target class
   * @return TypeConverter
   */
  @Override
  public MatchingConverter getConverter(Class<?> sourceType, TypeDescriptor targetType) {
    ConverterKey key = new ConverterKey(targetType, sourceType);
    MatchingConverter matchingConverter = converterMappings.get(key, targetType);
    if (matchingConverter != NO_MATCH) {
      return matchingConverter;
    }
    return null;
  }

  class ConverterMapCache extends MapCache<ConverterKey, MatchingConverter, TypeDescriptor> {

    @Override
    protected MatchingConverter createValue(ConverterKey key, TypeDescriptor targetType) {
      Class<?> sourceType = key.sourceType;

      for (MatchingConverter converter : converters) {
        if (converter.supports(targetType, sourceType)) {
          return converter;
        }
      }

      return NO_MATCH;
    }
  }

  static final class ConverterKey {
    private final int hash;
    final Class<?> sourceType;
    final TypeDescriptor targetType;

    ConverterKey(TypeDescriptor targetType, Class<?> sourceType) {
      this.targetType = targetType;
      this.sourceType = sourceType;
      this.hash = this.sourceType.hashCode() * 31 + this.targetType.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof final ConverterKey that))
        return false;
      return sourceType == that.sourceType
              && Objects.equals(targetType, that.targetType);
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }

  /**
   * Add {@link MatchingConverter} to {@link #converters}
   *
   * @param converters {@link MatchingConverter} object
   * @since 2.1.6
   */
  @Override
  public void addConverters(@Nullable MatchingConverter... converters) {
    if (ObjectUtils.isNotEmpty(converters)) {
      Collections.addAll(this.converters, converters);
      sort();
      invalidateCache();
    }
  }

  @Override
  public void addConverter(@Nullable MatchingConverter converter) {
    if (converter != null) {
      this.converters.add(converter);
      sort();
      invalidateCache();
    }
  }

  /**
   * Add a list of {@link MatchingConverter} to {@link #converters}
   *
   * @param converters {@link MatchingConverter} object
   * @since 2.1.6
   */
  @Override
  public void addConverters(@Nullable List<MatchingConverter> converters) {
    if (CollectionUtils.isNotEmpty(converters)) {
      this.converters.addAll(converters);
      invalidateCache();
      sort();
    }
  }

  public List<MatchingConverter> getConverters() {
    return converters;
  }

  @Override
  public void setConverters(@Nullable MatchingConverter... converters) {
    this.converters.clear();
    invalidateCache();

    if (ObjectUtils.isNotEmpty(converters)) {
      CollectionUtils.addAll(this.converters, converters);
    }
    sort();
  }

  /**
   * @since 4.0
   */
  private void sort() {
    AnnotationAwareOrderComparator.sort(converters);
  }

  @Override
  public void addConverters(Converter<?, ?>... converters) {
    if (ObjectUtils.isNotEmpty(converters)) {
      for (Converter<?, ?> converter : converters) {
        addConverter(converter);
      }
    }
  }

  @Override
  public <S, T> void addConverter(Converter<S, T> converter) {
    if (converter instanceof TypeCapable) {
      addConverter((TypeCapable) converter, converter);
    }
    else {
      Assert.notNull(converter, "converter must not be null");
      Class<?>[] generics = GenericTypeResolver.resolveTypeArguments(converter.getClass(), Converter.class);
      if (ObjectUtils.isNotEmpty(generics)) {
        Class<T> targetType = (Class<T>) generics[1];
        Class<S> sourceType = (Class<S>) generics[0];
        addConverter(targetType, sourceType, converter);
      }
      else {
        throw new IllegalArgumentException("can't get converter's generics: " + converter);
      }
    }
  }

  @SuppressWarnings({ "rawtypes" })
  public void addConverter(TypeCapable typeCapable, Converter converter) {
    Assert.notNull(converter, "converter must not be null");
    Class<?> targetType = typeCapable.getTargetType();
    Assert.state(targetType != null, "targetType must not be null");

    Class<?>[] sourceTypes = typeCapable.getSourceTypes();
    if (sourceTypes == null) {
      addConverter(targetType, converter);
    }
    else {
      addConverter(new MatchingConverterAdapter(targetType, converter, sourceTypes));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <S, T> void addConverter(Class<T> targetType, Converter<? super S, ? extends T> converter) {
    ResolvableType type = ResolvableType.fromInstance(converter).as(Converter.class);
    if (type.hasGenerics()) {
      ResolvableType generic = type.getGeneric(0);
      addConverter(targetType, (Class<S>) generic.toClass(), converter);
    }
    else {
      throw new IllegalArgumentException("can't get converter's generics: " + converter);
    }
  }

  @Override
  public <S, T> void addConverter(
          Class<T> targetType, Class<S> sourceType, Converter<? super S, ? extends T> converter) {
    Assert.notNull(converter, "converter must not be null");
    Assert.notNull(targetType, "targetType must not be null");
    Assert.notNull(sourceType, "sourceType must not be null");

    this.converters.add(new GenericConverter(targetType, sourceType, converter));

    sort();
    invalidateCache();
  }

  void invalidateCache() {
    this.converterMappings.clear();
  }

  // static

  public static DefaultConversionService getSharedInstance() {
    return sharedInstance;
  }

  /**
   * Add converters appropriate for most environments.
   *
   * @param registry the registry of converters to add to
   * (must also be castable to ConversionService, e.g. being a {@link ConfigurableConversionService})
   * @throws ClassCastException if the given ConverterRegistry could not be cast to a ConversionService
   */
  public static void addDefaultConverters(ConverterRegistry registry) {
    addScalarConverters(registry);
    addCollectionConverters(registry);

    registry.addConverters(new StringToTimeZoneConverter(),
            new ZoneIdToTimeZoneConverter(),
            new ZonedDateTimeToCalendarConverter());

    registry.addConverters(
            new StringToResourceConverter(),
            new ByteBufferConverter((ConversionService) registry),
            new IdToEntityConverter((ConversionService) registry),
            new ObjectToOptionalConverter((ConversionService) registry),

            // @since 4.0
            new ObjectToSupplierConverter((ConversionService) registry),

            new PrimitiveClassConverter(),

            new ObjectToObjectConverter(),
            new FallbackConverter()
    );

  }

  /**
   * Add common collection converters.
   *
   * @param registry the registry of converters to add to
   * (must also be castable to ConversionService, e.g. being a {@link ConfigurableConversionService})
   * @throws ClassCastException if the given ConverterRegistry could not be cast to a ConversionService
   */
  public static void addCollectionConverters(ConverterRegistry registry) {
    ConversionService conversionService = (ConversionService) registry;

    registry.addConverters(new ArrayToCollectionConverter(conversionService),
            new CollectionToArrayConverter(conversionService),

            new ArrayToArrayConverter(conversionService),
            new CollectionToCollectionConverter(conversionService),
            new MapToMapConverter(conversionService),

            new ArrayToStringConverter(conversionService),
            new StringToArrayConverter(conversionService),

            new ArrayToObjectConverter(conversionService),
            new ObjectToArrayConverter(conversionService),

            new CollectionToStringConverter(conversionService),
            new StringToCollectionConverter(conversionService),

            new CollectionToObjectConverter(conversionService),
            new ObjectToCollectionConverter(conversionService),

            new StreamConverter(conversionService));

  }

  private static void addScalarConverters(ConverterRegistry registry) {

    registry.addConverters(
            new IntegerConverter(int.class),
            new IntegerConverter(Integer.class),
            new LongConverter(Long.class),
            new LongConverter(long.class),
            new DoubleConverter(Double.class),
            new DoubleConverter(double.class),
            new FloatConverter(float.class),
            new FloatConverter(Float.class),
            new ByteConverter(Byte.class),
            new ByteConverter(byte.class),
            new ShortConverter(short.class),
            new ShortConverter(Short.class),
            new BigDecimalConverter(BigDecimal.class),
            new BigDecimalConverter(Number.class)
    );

    registry.addConverters(
            new StringToCharacterConverter(char.class),
            new StringToCharacterConverter(Character.class),

            new ClassConverter(),
            new CharsetConverter(),
            new DurationConverter(),
            new DataSizeConverter(),
            new MimeTypeConverter(),
            new MediaTypeConverter()

    );

    registry.addConverter(String.class, Number.class, ObjectToStringConverter.INSTANCE);
    registry.addConverter(String.class, Character.class, ObjectToStringConverter.INSTANCE);

    registry.addConverter(new NumberToCharacterConverter());

    registry.addConverter(new StringToBooleanConverter());

    registry.addConverter(String.class, Boolean.class, ObjectToStringConverter.INSTANCE);

    registry.addConverters(new StringToEnumConverter());
    registry.addConverter(new EnumToStringConverter());

    registry.addConverters(new IntegerToEnumConverter());

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
   * 2019-06-19 12:28
   */
  static class PrimitiveClassConverter implements MatchingConverter {

    @Override
    public boolean supports(TypeDescriptor targetType, Class<?> source) {

      Class<?> targetClass;
      if (targetType.isArray()) {
        targetClass = targetType.getComponentType();
      }
      else {
        targetClass = targetType.getType();
      }

      return (targetClass == boolean.class && source == Boolean.class) //
              || (targetClass == long.class && source == Long.class)//
              || (targetClass == int.class && source == Integer.class)//
              || (targetClass == float.class && source == Float.class)//
              || (targetClass == short.class && source == Short.class)//
              || (targetClass == double.class && source == Double.class)//
              || (targetClass == char.class && source == Character.class)//
              || (targetClass == byte.class && source == Byte.class);
    }

    @Override
    public Object convert(TypeDescriptor targetClass, Object source) {
      return source; // auto convert
    }
  }

  static class NopMatchingConverter implements MatchingConverter {

    @Override
    public boolean supports(TypeDescriptor targetType, Class<?> sourceType) {
      return false;
    }

    @Override
    public Object convert(TypeDescriptor targetType, Object source) {
      return source;
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  record MatchingConverterAdapter(Class<?> targetType, Converter converter, Class<?>[] sourceTypes)
          implements MatchingConverter {

    @Override
    public boolean supports(TypeDescriptor targetType, Class<?> sourceType) {
      if (targetType.is(this.targetType)) {
        for (Class<?> type : this.sourceTypes) {
          if (type == sourceType || ClassUtils.isAssignable(type, sourceType)) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public Object convert(TypeDescriptor targetType, Object source) {
      return converter.convert(source);
    }

  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  record GenericConverter(Class<?> targetType, Class<?> sourceType, Converter converter)
          implements MatchingConverter, Ordered {

    @Override
    public boolean supports(TypeDescriptor targetType, Class<?> sourceType) {
      return targetType.is(this.targetType) && (
              this.sourceType == sourceType || this.sourceType.isAssignableFrom(sourceType)
      );
    }

    @Override
    public Object convert(TypeDescriptor targetType, Object source) {
      return converter.convert(source);
    }

    @Override
    public int getOrder() {
      return HIGHEST_PRECEDENCE;
    }
  }

  /**
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    converters.trimToSize();
  }

}
