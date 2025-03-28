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

package infra.core.conversion.support;

import infra.core.conversion.Converter;
import infra.lang.Nullable;

/**
 * Converts a String to a Character.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class StringToCharacterConverter implements Converter<String, Character> {

  @Override
  @Nullable
  public Character convert(String source) {
    if (source.isEmpty()) {
      return null;
    }
    if (source.length() > 1) {
      throw new IllegalArgumentException(
              "Can only convert a [String] with length of 1 to a [Character]; string value '" + source + "'  has length of " + source.length());
    }
    return source.charAt(0);
  }

}
