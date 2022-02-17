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

  /**
   * Add a plain converter to this registry.
   * The convertible source/target type pair is derived from the Converter's parameterized types.
   *
   * @throws IllegalArgumentException if the parameterized types could not be resolved
   */
  void addConverter(Converter<?, ?> converter);

  /**
   * Add a plain converter to this registry.
   * The convertible source/target type pair is specified explicitly.
   * <p>Allows for a Converter to be reused for multiple distinct pairs without
   * having to create a Converter class for each pair.
   */
  <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter);

  /**
   * Add a generic converter to this registry.
   *
   * @since 4.0
   */
  void addConverter(GenericConverter converter);

  /**
   * Add a ranged converter factory to this registry.
   * The convertible source/target type pair is derived from the ConverterFactory's parameterized types.
   *
   * @throws IllegalArgumentException if the parameterized types could not be resolved
   * @since 4.0
   */
  void addConverterFactory(ConverterFactory<?, ?> factory);

  /**
   * Remove any converters from {@code sourceType} to {@code targetType}.
   *
   * @param sourceType the source type
   * @param targetType the target type
   * @since 4.0
   */
  void removeConvertible(Class<?> sourceType, Class<?> targetType);

}
