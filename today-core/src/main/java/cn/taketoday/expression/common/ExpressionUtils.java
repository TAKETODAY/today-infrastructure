/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.expression.common;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Common utility functions that may be used by any Expression Language provider.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class ExpressionUtils {

  /**
   * Determines if there is a type converter available in the specified context and
   * attempts to use it to convert the supplied value to the specified type. Throws an
   * exception if conversion is not possible.
   *
   * @param context the evaluation context that may define a type converter
   * @param typedValue the value to convert and a type descriptor describing it
   * @param targetType the type to attempt conversion to
   * @return the converted value
   * @throws EvaluationException if there is a problem during conversion or conversion
   * of the value to the specified type is not supported
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T convertTypedValue(
          @Nullable EvaluationContext context, TypedValue typedValue, @Nullable Class<T> targetType) {

    Object value = typedValue.getValue();
    if (targetType == null) {
      return (T) value;
    }
    if (context != null) {
      return (T) context.getTypeConverter().convertValue(
              value, typedValue.getTypeDescriptor(), TypeDescriptor.valueOf(targetType));
    }
    if (ClassUtils.isAssignableValue(targetType, value)) {
      return (T) value;
    }
    throw new EvaluationException("Cannot convert value '" + value + "' to type '" + targetType.getName() + "'");
  }

  /**
   * Attempt to convert a typed value to an int using the supplied type converter.
   */
  public static int toInt(TypeConverter typeConverter, TypedValue typedValue) {
    return convertValue(typeConverter, typedValue, Integer.class);
  }

  /**
   * Attempt to convert a typed value to a boolean using the supplied type converter.
   */
  public static boolean toBoolean(TypeConverter typeConverter, TypedValue typedValue) {
    return convertValue(typeConverter, typedValue, Boolean.class);
  }

  /**
   * Attempt to convert a typed value to a double using the supplied type converter.
   */
  public static double toDouble(TypeConverter typeConverter, TypedValue typedValue) {
    return convertValue(typeConverter, typedValue, Double.class);
  }

  /**
   * Attempt to convert a typed value to a long using the supplied type converter.
   */
  public static long toLong(TypeConverter typeConverter, TypedValue typedValue) {
    return convertValue(typeConverter, typedValue, Long.class);
  }

  /**
   * Attempt to convert a typed value to a char using the supplied type converter.
   */
  public static char toChar(TypeConverter typeConverter, TypedValue typedValue) {
    return convertValue(typeConverter, typedValue, Character.class);
  }

  /**
   * Attempt to convert a typed value to a short using the supplied type converter.
   */
  public static short toShort(TypeConverter typeConverter, TypedValue typedValue) {
    return convertValue(typeConverter, typedValue, Short.class);
  }

  /**
   * Attempt to convert a typed value to a float using the supplied type converter.
   */
  public static float toFloat(TypeConverter typeConverter, TypedValue typedValue) {
    return convertValue(typeConverter, typedValue, Float.class);
  }

  /**
   * Attempt to convert a typed value to a byte using the supplied type converter.
   */
  public static byte toByte(TypeConverter typeConverter, TypedValue typedValue) {
    return convertValue(typeConverter, typedValue, Byte.class);
  }

  @SuppressWarnings("unchecked")
  private static <T> T convertValue(TypeConverter typeConverter, TypedValue typedValue, Class<T> targetType) {
    Object result = typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
            TypeDescriptor.valueOf(targetType));
    if (result == null) {
      throw new IllegalStateException("Null conversion result for value [" + typedValue.getValue() + "]");
    }
    return (T) result;
  }

}
