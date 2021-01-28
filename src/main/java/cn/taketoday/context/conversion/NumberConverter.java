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

import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.Assert;

/**
 * @author TODAY
 * 2021/1/6 23:18
 */
public class NumberConverter implements Converter<Object, Number>, TypeCapable {

  private final Class<?> type;
  private final boolean primitive;

  public NumberConverter(Class<?> targetClass) {
    Assert.notNull(targetClass, "targetClass must not be null");
    this.type = targetClass;
    this.primitive = targetClass.isPrimitive();
  }

  @Override
  public final Number convert(Object source) {
    if (source == null) {
      return convertNull();
    }
    else if (source instanceof Number) {
      return convertNumber((Number) source);
    }
    else if (source instanceof String) {
      return convertString((String) source);
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
    throw new ConversionException("Not support source: '" + source + "' convert to target class: " + type);
  }

  public boolean isPrimitive() {
    return primitive;
  }

  @Override
  public Class<?> getType() {
    return type;
  }

}
