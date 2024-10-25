/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.core.conversion.support;

import java.util.Locale;
import java.util.Set;

import cn.taketoday.core.conversion.Converter;
import cn.taketoday.lang.Nullable;

/**
 * Converts String to a Boolean.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class StringToBooleanConverter implements Converter<String, Boolean> {

  private static final Set<String> trueValues = Set.of("true", "on", "yes", "1");
  private static final Set<String> falseValues = Set.of("false", "off", "no", "0");

  @Override
  @Nullable
  public Boolean convert(String source) {
    String value = source.trim();
    if (value.isEmpty()) {
      return null;
    }
    value = value.toLowerCase(Locale.ROOT);
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
