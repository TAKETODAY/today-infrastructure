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

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.MatchingConverter;

/**
 * @author TODAY <br>
 * 2019-06-06 15:31
 * @since 2.1.6
 */
public abstract class StringSourceMatchingConverter implements MatchingConverter {

  @Override
  public final boolean supports(final TypeDescriptor targetType, final Class<?> sourceType) {
    return sourceType == String.class
            && supportsInternal(targetType, sourceType);
  }

  public boolean supportsInternal(TypeDescriptor targetType, Class<?> sourceType) {
    return true;
  }

  @Override
  public final Object convert(TypeDescriptor targetType, Object source) {
    return convertInternal(targetType, (String) source);
  }

  protected abstract Object convertInternal(TypeDescriptor targetClass, String source);
}
