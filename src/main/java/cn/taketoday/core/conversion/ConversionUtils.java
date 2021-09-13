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
package cn.taketoday.core.conversion;

import java.time.Duration;
import java.util.List;

import cn.taketoday.core.Assert;
import cn.taketoday.core.conversion.support.DefaultConversionService;

/**
 * @author TODAY <br>
 * 2018-07-12 20:43:53
 */
public abstract class ConversionUtils {

  public static boolean supports(Object source, Class<?> targetClass) {
    return DefaultConversionService.getSharedInstance().canConvert(source, targetClass);
  }

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
  public static TypeConverter getConverter(Object source, Class<?> targetClass) {
    return DefaultConversionService.getSharedInstance().getConverter(source, targetClass);
  }

  /**
   * Convert source to target type
   *
   * @param source
   *         value
   * @param targetClass
   *         targetClass
   *
   * @return converted object
   */
  public static Object convert(final Object source, final Class<?> targetClass) {
    return DefaultConversionService.getSharedInstance().convert(source, targetClass);
  }

  /**
   * @param <T>
   *         Target type
   * @param targetClass
   *         Target type
   * @param source
   *         Source object
   *
   * @return converted object
   *
   * @since 2.1.7
   */
  @SuppressWarnings("unchecked")
  public static <T> T convert(Class<T> targetClass, Object source) {
    return (T) convert(source, targetClass);
  }

  public static List<TypeConverter> getConverters() {
    return DefaultConversionService.getSharedInstance().getConverters();
  }

  public static void setConverters(TypeConverter... cts) {
    DefaultConversionService.getSharedInstance().setConverters(cts);
  }

  /**
   * Convert a string to {@link Duration}
   *
   * @param value
   *         Input string
   */
  public static Duration parseDuration(String value) {
    Assert.notNull(value, "Input string must not be null");
    if (value.endsWith("ns")) {
      return Duration.ofNanos(getDuration(value, 2));
    }
    if (value.endsWith("ms")) {
      return Duration.ofMillis(getDuration(value, 2));
    }
    if (value.endsWith("min")) {
      return Duration.ofMinutes(getDuration(value, 3));
    }
    if (value.endsWith("s")) {
      return Duration.ofSeconds(getDuration(value, 1));
    }
    if (value.endsWith("h")) {
      return Duration.ofHours(getDuration(value, 1));
    }
    if (value.endsWith("d")) {
      return Duration.ofDays(getDuration(value, 1));
    }

    return Duration.parse(value);
  }

  private static long getDuration(String value, int i) {
    return Long.parseLong(value.substring(0, value.length() - i));
  }

  /**
   * Add {@link TypeConverter} to {@link DefaultConversionService#converters}
   *
   * @param converters
   *         {@link TypeConverter} object
   *
   * @since 2.1.6
   */
  public static void addConverter(TypeConverter... converters) {
    DefaultConversionService.getSharedInstance().addConverters(converters);
  }

  /**
   * Add a list of {@link TypeConverter} to {@link DefaultConversionService#converters}
   *
   * @param converters
   *         {@link TypeConverter} object
   *
   * @since 2.1.6
   */
  public static void addConverter(List<TypeConverter> converters) {
    DefaultConversionService.getSharedInstance().addConverters(converters);
  }

  public static void addConverters(final Converter<?, ?>... converters) {
    DefaultConversionService.getSharedInstance().addConverters(converters);
  }

  public static <S, T> void addConverter(final Converter<S, T> converter) {
    DefaultConversionService.getSharedInstance().addConverter(converter);
  }

  public static <S, T> void addConverter(Class<T> targetType, Converter<? super S, ? extends T> converter) {
    DefaultConversionService.getSharedInstance().addConverter(targetType, converter);
  }

  public static <S, T> void addConverter(
          Class<T> targetType, Class<S> sourceType, Converter<? super S, ? extends T> converter) {
    DefaultConversionService.getSharedInstance().addConverter(targetType, sourceType, converter);
  }
}
