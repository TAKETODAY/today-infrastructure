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

package cn.taketoday.context.conversion;

import cn.taketoday.context.GenericDescriptor;
import cn.taketoday.context.TypeReference;

/**
 * Conversion Service
 * <p>
 * Use {@link Converter} to convert
 * </p>
 *
 * @author TODAY 2021/3/19 20:59
 * @since 3.0
 */
public interface ConversionService {
  /**
   * use {@link GenericDescriptor} to resolve generic info
   */
  boolean canConvert(Class<?> sourceType, GenericDescriptor targetType);

  /**
   * static test this {@link ConversionService} can convert source to target
   */
  default boolean canConvert(Class<?> sourceType, Class<?> targetType) {
    return canConvert(sourceType, GenericDescriptor.ofClass(targetType));
  }

  /**
   * whether this {@link ConversionService} supports to convert source object to
   * target class object
   *
   * @param targetType
   *         target class
   * @param source
   *         source object
   *
   * @return whether this {@link ConversionService} supports to convert source object
   * to target class object
   */
  default boolean canConvert(Object source, Class<?> targetType) {
    return getConverter(source, targetType) != null;
  }

  /**
   * Convert source to target type
   * <p>
   * If source object is {@code null} just returns {@code null}
   * </p>
   *
   * @param source
   *         source object
   * @param targetClass
   *         targetClass
   *
   * @return converted object
   */
  default <T> T convert(Object source, Class<T> targetClass) {
    return convert(source, GenericDescriptor.ofClass(targetClass));
  }

  /**
   * Convert source to target type
   * <p>
   * If source object is {@code null} just returns {@code null}
   * </p>
   *
   * @param source
   *         source object
   * @param targetType
   *         target class and generics info
   */
  <T> T convert(Object source, GenericDescriptor targetType);

  <T> T convert(Object source, GenericDescriptor sourceDescriptor, GenericDescriptor targetType);

  TypeConverter getConverter(Class<?> sourceType, GenericDescriptor targetType);

  default TypeConverter getConverter(Object object, GenericDescriptor targetType) {
    return getConverter(object.getClass(), targetType);
  }

  /**
   * Get Target {@link TypeConverter}
   *
   * @param source
   *         input source
   * @param targetType
   *         convert to target class
   *
   * @return TypeConverter
   */
  default TypeConverter getConverter(Object source, Class<?> targetType) {
    return getConverter(source.getClass(), targetType);
  }

  default TypeConverter getConverter(Class<?> sourceType, Class<?> targetType) {
    return getConverter(sourceType, GenericDescriptor.ofClass(targetType));
  }

}
