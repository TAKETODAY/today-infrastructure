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

import java.util.HashSet;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.MatchingConverter;

/**
 * Converts String to a Boolean.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY
 * @since 3.0
 */
public final class StringToBooleanConverter extends StringSourceMatchingConverter implements MatchingConverter {
  public static final HashSet<String> trueValues = new HashSet<>(8);
  public static final HashSet<String> falseValues = new HashSet<>(8);

  static {
    trueValues.add("true");
    trueValues.add("on");
    trueValues.add("yes");
    trueValues.add("1");

    falseValues.add("false");
    falseValues.add("off");
    falseValues.add("no");
    falseValues.add("0");
  }

  @Override
  public boolean supportsInternal(TypeDescriptor targetType, Class<?> sourceType) {
    return targetType.is(boolean.class) || targetType.is(Boolean.class);
  }

  @Override
  protected Object convertInternal(final TypeDescriptor targetType, final String source) {
    String value = source.trim();
    if (value.isEmpty()) {
      if (targetType.is(Boolean.class)) {
        return null;
      }
      return Boolean.FALSE;
    }
    value = value.toLowerCase();
    if (trueValues.contains(value)) {
      return Boolean.TRUE;
    }
    else if (falseValues.contains(value)) {
      return Boolean.FALSE;
    }
    else {
      throw new IllegalArgumentException("Invalid boolean value '" + source + "'");
    }
  }
}
