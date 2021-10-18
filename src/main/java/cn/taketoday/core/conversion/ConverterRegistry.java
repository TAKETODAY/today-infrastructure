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

package cn.taketoday.core.conversion;

import java.util.List;

/**
 * @author TODAY 2021/3/21 17:55
 * @since 3.0
 */
public interface ConverterRegistry {

  void setConverters(TypeConverter... cts);

  /**
   * Add {@link TypeConverter}s
   *
   * @param converters {@link TypeConverter} object
   */
  void addConverters(TypeConverter... converters);

  void addConverter(TypeConverter converter);

  /**
   * Add a list of {@link TypeConverter}
   *
   * @param converters {@link TypeConverter} object
   */
  void addConverters(List<TypeConverter> converters);

  // Converter

  <S, T> void addConverter(Converter<S, T> converter);

  void addConverters(Converter<?, ?>... converters);

  <S, T> void addConverter(
          Class<T> targetType, Converter<? super S, ? extends T> converter);

  <S, T> void addConverter(
          Class<T> targetType, Class<S> sourceType, Converter<? super S, ? extends T> converter);

}
