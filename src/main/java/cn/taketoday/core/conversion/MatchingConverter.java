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

import cn.taketoday.core.TypeDescriptor;

/**
 * Matching converter
 *
 * @author TODAY 2019-06-06 14:17
 * @see #supports(TypeDescriptor, Class)
 * @since 2.1.6
 */
public interface MatchingConverter {

  /**
   * whether this {@link MatchingConverter} supports to convert source object to
   * target class object
   *
   * @param targetType target class
   * @param sourceType source object never be null
   * @return whether this {@link MatchingConverter} supports to convert source object
   * to target class object
   */
  boolean supports(TypeDescriptor targetType, Class<?> sourceType);

  /**
   * Convert source object to target object
   *
   * @param targetType target type
   * @param source source object never be null
   * @return a converted object
   * @throws ConversionException if can't convert to target object
   */
  Object convert(TypeDescriptor targetType, Object source);

}
