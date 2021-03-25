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
package cn.taketoday.context.conversion.support;

import cn.taketoday.context.conversion.AbstractTypeCapable;
import cn.taketoday.context.conversion.Converter;

/**
 * Converts a String to a Character.
 *
 * @author Keith Donald
 * @author TODAY
 * @since 3.0
 */
final class StringToCharacterConverter
        extends AbstractTypeCapable implements Converter<String, Character> {

  protected StringToCharacterConverter(Class<?> type) {
    super(type);
  }

  @Override
  public Character convert(final String source) {
    if (source.isEmpty()) {
      return null;
    }
    if (source.length() > 1) {
      throw new IllegalArgumentException(
              "Can only convert a [String] with length of 1 to a [Character]; string value '"
                      + source + "'  has length of " + source.length());
    }
    return source.charAt(0);
  }

}
