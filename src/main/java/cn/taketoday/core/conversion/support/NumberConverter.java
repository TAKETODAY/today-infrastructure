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

import java.lang.reflect.Array;
import java.util.Collection;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.AbstractTypeCapable;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.MatchingConverter;

/**
 * Support source is String, Number, Character, Enum, Collection, Array to
 * target number type
 *
 * @author TODAY 2021/1/6 23:18
 * @since 3.0
 */
public class NumberConverter
        extends AbstractTypeCapable implements MatchingConverter {

  private final boolean primitive;

  public NumberConverter(Class<?> targetClass) {
    super(targetClass);
    this.primitive = targetClass.isPrimitive();
  }

  /**
   * @param targetType target class
   * @param sourceType source object never be null
   * @since 4.0
   */
  @Override
  public boolean supports(
          TypeDescriptor targetType, Class<?> sourceType) {
    return targetType.is(this.targetType) && (
            sourceType == String.class
                    || Number.class.isAssignableFrom(sourceType)
                    || Character.class == sourceType
                    || Enum.class.isAssignableFrom(sourceType)
                    || sourceType.isArray()
                    || Collection.class.isAssignableFrom(sourceType)
    );
  }

  @Override
  public final Object convert(TypeDescriptor targetType, Object source) {
    return convert(source);
  }

  public final Number convert(Object source) {
    if (source == null) {
      return convertNull();
    }
    else if (source instanceof Number) {
      return convertNumber((Number) source);
    }
    else if (source instanceof final String stringSource) {
      if (stringSource.isEmpty()) {
        return convertNull(); // fix @since 3.0.4
      }
      try {
        return convertString(stringSource);
      }
      catch (NumberFormatException e) {
        throw new ConversionException("Can't convert a string: '" + source + "' to a number", e);
      }
    }
    return convertObject(source);
  }

  protected Number convertNull() {
    return primitive ? convertNumber(0) : null;
  }

  protected Number convertNumber(Number source) {
    return source.intValue(); // int
  }

  protected Number convertString(String source) {
    final String stringVal = source.trim();
    return stringVal.isEmpty() ? convertNull() : parseString(stringVal);
  }

  protected Number parseString(String stringVal) {
    return Integer.parseInt(stringVal);
  }

  protected Number convertObject(Object source) {
    if (source.getClass().isArray() && Array.getLength(source) > 0) {
      return convert(Array.get(source, 0));
    }
    if (source instanceof Collection) {
      final Object next = ((Collection<?>) source).iterator().next();
      return convert(next);
    }
    if (source instanceof final Character character) {
      return convertNumber((short) character.charValue());
    }
    if (source instanceof Enum) {
      return convertNumber(((Enum<?>) source).ordinal());
    }
    throw new ConversionException(
            "Not support source: '" + source + "' convert to target class: " + targetType);
  }

  public boolean isPrimitive() {
    return primitive;
  }

}
