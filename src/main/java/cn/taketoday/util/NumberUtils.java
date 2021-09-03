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
package cn.taketoday.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import cn.taketoday.core.conversion.ConversionException;

/**
 * @author TODAY <br>
 * 2018-07-06 13:36:29
 */
public abstract class NumberUtils {

  /**
   * parse text to target number
   *
   * @param text
   *         source text
   * @param targetClass
   *         target number class
   *
   * @return Number object
   *
   * @throws ConversionException
   *         Can't convert text to targetClass
   */
  public static Object parseDigit(String text, Class<?> targetClass) {

    if (StringUtils.isEmpty(text)) {
      return 0;
    }
    if (Byte.class == targetClass || byte.class == targetClass) {
      return Byte.valueOf(text);
    }
    else if (Short.class == targetClass || short.class == targetClass) {
      return Short.valueOf(text);
    }
    else if (Integer.class == targetClass || int.class == targetClass) {
      return Integer.valueOf(text);
    }
    else if (Long.class == targetClass || long.class == targetClass) {
      return Long.valueOf(text);
    }
    else if (BigInteger.class == targetClass) {
      return new BigInteger(text);
    }
    else if (Float.class == targetClass || float.class == targetClass) {
      return Float.valueOf(text);
    }
    else if (Double.class == targetClass || double.class == targetClass) {
      return Double.valueOf(text);
    }
    else if (BigDecimal.class == targetClass || Number.class == targetClass) {
      return BigDecimal.valueOf(Double.parseDouble(text));
    }
    throw new ConversionException("can't convert[" + text + "] to [" + targetClass.getName() + "]");
  }

  /**
   * Is a number?
   *
   * @param targetClass
   *         the target class
   */
  public static boolean isNumber(Class<?> targetClass) {
    return Number.class.isAssignableFrom(targetClass) //
            || targetClass == int.class//
            || targetClass == long.class//
            || targetClass == float.class//
            || targetClass == double.class//
            || targetClass == short.class//
            || targetClass == byte.class;
  }

}
