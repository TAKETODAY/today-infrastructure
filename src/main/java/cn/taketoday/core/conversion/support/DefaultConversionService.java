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

import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.conversion.ConfigurableConversionService;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.conversion.ConverterRegistry;
import cn.taketoday.core.conversion.TypeCapable;
import cn.taketoday.core.conversion.TypeConverter;
import cn.taketoday.util.GenericDescriptor;
import cn.taketoday.util.GenericTypeResolver;
import cn.taketoday.util.Mappings;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.OrderUtils;
import cn.taketoday.util.ResolvableType;

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

  private final ArrayList<TypeConverter> converters = new ArrayList<>();
  private final ConverterMappings converterMappings = new ConverterMappings();
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
  public boolean canConvert(Class<?> sourceType, GenericDescriptor targetType) {
    return getConverter(sourceType, targetType) != null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T convert(final Object source, final GenericDescriptor targetType) {
    if (source == null) {
      return convertNull(targetType);
    }
    Assert.notNull(targetType, "targetType must not be null");
    final TypeConverter typeConverter = getConverter(source.getClass(), targetType);
    if (typeConverter == null) {
      return handleConverterNotFound(source, targetType);
    }
    try {
      return (T) typeConverter.convert(targetType, source);
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
          Object source, GenericDescriptor targetType) throws ConverterNotFoundException {
    if (targetType.isInstance(source)) {
      return (T) source;
    }
    throw new ConverterNotFoundException(source, targetType);
  }

  @SuppressWarnings("unchecked")
  protected <T> T convertNull(final GenericDescriptor targetType) {
    return (T) nullMappings.get(targetType.getType());
  }

  /**
   * add null value mapping
   *
   * @see #convertNull(GenericDescriptor)
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
   * Get Target {@link TypeConverter}
   *
   * @param sourceType
   *         input sourceType
   * @param targetType
   *         convert to target class
   *
   * @return TypeConverter
   */
  @Override
  public TypeConverter getConverter(final Class<?> sourceType, final GenericDescriptor targetType) {
    final ConverterKey key = new ConverterKey(targetType, sourceType);
    final TypeConverter typeConverter = converterMappings.get(key, targetType);
    if (typeConverter != NO_MATCH) {
      return typeConverter;
    }
    return null;
  }

  class ConverterMappings extends Mappings<TypeConverter, GenericDescriptor> {

    @Override
    protected TypeConverter createValue(final Object key, final GenericDescriptor targetType) {
      final Class<?> sourceType = ((ConverterKey) key).sourceType;

      for (final TypeConverter converter : converters) {
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
    final GenericDescriptor targetType;

    ConverterKey(GenericDescriptor targetType, Class<?> sourceType) {
      this.targetType = targetType;
      this.sourceType = sourceType;
      this.hash = this.sourceType.hashCode() * 31 + this.targetType.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof ConverterKey))
        return false;
      final ConverterKey that = (ConverterKey) o;
      return sourceType == that.sourceType
              && Objects.equals(targetType, that.targetType);
    }

    @Override
    public int hashCode() {
      return hash;
    }
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
  public void addConverters(final TypeConverter... converters) {
    if (ObjectUtils.isNotEmpty(converters)) {
      Collections.addAll(this.converters, converters);
      OrderUtils.reversedSort(this.converters);
      invalidateCache();
    }
  }

  @Override
  public void addConverter(TypeConverter converter) {
    this.converters.add(converter);

    OrderUtils.reversedSort(this.converters);
    invalidateCache();
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
  public void addConverters(final List<TypeConverter> converters) {
    if (ObjectUtils.isNotEmpty(converters)) {
      this.converters.addAll(converters);
      OrderUtils.reversedSort(this.converters);
      invalidateCache();
    }
  }

  public List<TypeConverter> getConverters() {
    return converters;
  }

  @Override
  public void setConverters(final TypeConverter... converters) {
    Assert.notNull(converters, "TypeConverter must not be null");
    this.converters.clear();
    invalidateCache();
    Collections.addAll(this.converters, OrderUtils.reversedSort(converters));
  }

  @Override
  public void addConverters(final Converter<?, ?>... converters) {
    if (ObjectUtils.isNotEmpty(converters)) {
      for (final Converter<?, ?> converter : converters) {
        addConverter(converter);
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <S, T> void addConverter(final Converter<S, T> converter) {
    if (converter instanceof TypeCapable) {
      final Class<T> targetType = (Class<T>) ((TypeCapable) converter).getType();
      addConverter(targetType, converter);
    }
    else {
      Assert.notNull(converter, "converter must not be null");
      final Class<?>[] generics = GenericTypeResolver.resolveTypeArguments(converter.getClass(), Converter.class);
      if (ObjectUtils.isNotEmpty(generics)) {
        final Class<T> targetType = (Class<T>) generics[1];
        final Class<S> sourceType = (Class<S>) generics[0];
        addConverter(targetType, sourceType, converter);
      }
      else {
        throw new ConfigurationException("can't register get converter's target class");
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <S, T> void addConverter(Class<T> targetType, Converter<? super S, ? extends T> converter) {
    Assert.notNull(converter, "converter must not be null");
    ResolvableType type = ResolvableType.forClass(converter.getClass()).as(Converter.class);
    if (type.hasGenerics()) {
      final ResolvableType generic = type.getGeneric(0);
      addConverter(targetType, (Class<S>) generic.toClass(), converter);
    }
    else
      throw new ConfigurationException("can't register get converter's source class");
  }

  @Override
  public <S, T> void addConverter(
          Class<T> targetType, Class<S> sourceType, Converter<? super S, ? extends T> converter) {
    Assert.notNull(converter, "converter must not be null");
    Assert.notNull(targetType, "targetType must not be null");
    Assert.notNull(sourceType, "sourceType must not be null");

    final GenericConverter genericConverter = new GenericConverter(targetType, sourceType, converter);
    this.converters.add(genericConverter);

    invalidateCache();
    // order support
    OrderUtils.reversedSort(this.converters);
  }

  void invalidateCache() {
    this.converterMappings.clear();
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

    registry.addConverters(
            new StringToResourceConverter(),
            new ByteBufferConverter((ConversionService) registry),
            new IdToEntityConverter((ConversionService) registry),
            new ObjectToOptionalConverter((ConversionService) registry),

            new PrimitiveClassConverter(),

            new ObjectToObjectConverter(),
            new FallbackConverter()
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
            new BigDecimalConverter(Number.class),

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
  static class PrimitiveClassConverter implements TypeConverter {

    @Override
    public boolean supports(GenericDescriptor targetType, Class<?> source) {

      final Class<?> targetClass;
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
    public Object convert(GenericDescriptor targetClass, Object source) {
      return source; // auto convert
    }
  }

  static class NopTypeConverter implements TypeConverter {

    @Override
    public boolean supports(GenericDescriptor targetType, Class<?> sourceType) {
      return false;
    }

    @Override
    public Object convert(GenericDescriptor targetType, Object source) {
      return source;
    }
  }

  static final class GenericConverter implements TypeConverter, Ordered {
    final Class<?> targetType;
    final Class<?> sourceType;
    final Converter converter;

    GenericConverter(Class<?> targetType, Class<?> sourceType, Converter converter) {
      this.converter = converter;
      this.targetType = targetType;
      this.sourceType = sourceType;
    }

    @Override
    public boolean supports(final GenericDescriptor targetType, final Class<?> sourceType) {
      return targetType.is(this.targetType) && (
              this.sourceType == sourceType || this.sourceType.isAssignableFrom(sourceType)
      );
    }

    @Override
    public Object convert(GenericDescriptor targetType, Object source) {
      return converter.convert(source);
    }

    @Override
    public int getOrder() {
      return HIGHEST_PRECEDENCE;
    }
  }

}
