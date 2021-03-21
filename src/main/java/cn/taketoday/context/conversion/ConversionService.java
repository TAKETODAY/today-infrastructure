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
   * whether this {@link ConversionService} supports to convert source object to
   * target class object
   *
   * @param targetClass
   *         target class
   * @param source
   *         source object
   *
   * @return whether this {@link ConversionService} supports to convert source object
   * to target class object
   */
  default boolean canConvert(Object source, Class<?> targetClass) {
    return getConverter(source, targetClass) != null;
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
  <T> T convert(Object source, Class<T> targetClass);

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
  TypeConverter getConverter(Object source, Class<?> targetClass);

}
