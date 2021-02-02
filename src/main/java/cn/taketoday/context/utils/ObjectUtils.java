/**
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
package cn.taketoday.context.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import cn.taketoday.context.Constant;
import cn.taketoday.context.exception.ConversionException;

/**
 * @author TODAY <br>
 * 2019-08-23 00:16
 */
public abstract class ObjectUtils {

  /**
   * Determine whether the given object is an array:
   * either an Object array or a primitive array.
   *
   * @param obj
   *         the object to check
   *
   * @since 3.0
   */
  public static boolean isArray(Object obj) {
    return (obj != null && obj.getClass().isArray());
  }

  /**
   * Test if a array is a null or empty object
   *
   * @param array
   *         An array to test if its a null or empty object
   *
   * @return If a object is a null or empty object
   */
  public static boolean isEmpty(Object[] array) {
    return array == null || array.length == 0;
  }

  /**
   * Test if a object is a null or empty object
   *
   * @param obj
   *         A instance to test if its a null or empty object
   *
   * @return If a object is a null or empty object
   */
  public static boolean isEmpty(Object obj) {
    if (obj == null) {
      return true;
    }
    if (obj instanceof Optional) {
      return !((Optional<?>) obj).isPresent();
    }
    if (obj instanceof String) {
      return ((String) obj).isEmpty();
    }
    if (obj instanceof Collection) {
      return ((Collection<?>) obj).isEmpty();
    }
    if (obj instanceof Map) {
      return ((Map<?, ?>) obj).isEmpty();
    }
    return obj.getClass().isArray() && Array.getLength(obj) == 0;
  }

  public static boolean isNotEmpty(Object[] array) {
    return !isEmpty(array);
  }

  public static boolean isNotEmpty(Object obj) {
    return !isEmpty(obj);
  }

  /**
   * Unwrap the given object which is potentially a {@link java.util.Optional}.
   *
   * @param obj
   *         the candidate object
   *
   * @return either the value held within the {@code Optional}, {@code null}
   * if the {@code Optional} is empty, or simply the given object as-is
   *
   * @since 3.0
   */
  public static Object unwrapOptional(Object obj) {
    if (obj instanceof Optional) {
      Optional<?> optional = (Optional<?>) obj;
      if (!optional.isPresent()) {
        return null;
      }
      Object result = optional.get();
      Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
      return result;
    }
    return obj;
  }
  //

  /**
   * To array object
   *
   * @param source
   *         String array
   * @param targetClass
   *         Target class
   *
   * @return An array object
   *
   * @throws ConversionException
   *         If can't convert source to target type object
   */
  public static Object toArrayObject(String[] source, Class<?> targetClass) {

    // @since 2.1.6 fix: String[].class can't be resolve
    if (String[].class == targetClass) {
      return source;
    }
    final int length = source.length;
    if (int[].class == targetClass) {
      final int[] newInstance = new int[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Integer.parseInt(source[j]);
      return newInstance;
    }
    else if (Integer[].class == targetClass) {
      final Integer[] newInstance = new Integer[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Integer.valueOf(source[j]);
      return newInstance;
    }
    else if (long[].class == targetClass) {
      final long[] newInstance = new long[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Long.parseLong(source[j]);
      return newInstance;
    }
    else if (Long[].class == targetClass) {
      final Long[] newInstance = new Long[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Long.valueOf(source[j]);
      return newInstance;
    }
    else if (short[].class == targetClass) {
      final short[] newInstance = new short[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Short.parseShort(source[j]);
      return newInstance;
    }
    else if (Short[].class == targetClass) {
      final Short[] newInstance = new Short[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Short.valueOf(source[j]);
      return newInstance;
    }
    else if (byte[].class == targetClass) {
      final byte[] newInstance = new byte[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Byte.parseByte(source[j]);
      return newInstance;
    }
    else if (Byte[].class == targetClass) {
      final Byte[] newInstance = new Byte[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Byte.valueOf(source[j]);
      return newInstance;
    }
    else if (float[].class == targetClass) {
      final float[] newInstance = new float[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Float.parseFloat(source[j]);
      return newInstance;
    }
    else if (Float[].class == targetClass) {
      final Float[] newInstance = new Float[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Float.valueOf(source[j]);
      return newInstance;
    }
    else if (double[].class == targetClass) {
      final double[] newInstance = new double[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Double.parseDouble(source[j]);
      return newInstance;
    }
    else if (Double[].class == targetClass) {
      final Double[] newInstance = new Double[length];
      for (short j = 0; j < length; j++)
        newInstance[j] = Double.valueOf(source[j]);
      return newInstance;
    }
    { // fix @since 2.1.6
      if (targetClass.isArray()) {
        targetClass = targetClass.getComponentType();
      }
      final Object newInstance = Array.newInstance(targetClass, length);
      for (short i = 0; i < length; i++) {
        Array.set(newInstance, i, ConvertUtils.convert(source[i], targetClass));
      }
      return newInstance;
    }
  }

  public static <T> T parseArray(String[] source, Class<T> targetClass) {
    return targetClass.cast(toArrayObject(source, targetClass));
  }

  //---------------------------------------------------------------------
  // Convenience methods for toString output
  //---------------------------------------------------------------------

  public static String toHexString(final Object obj) {
    //        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    return obj == null
           ? "null"
           : new StringBuilder()
                   .append(obj.getClass().getName())
                   .append('@')
                   .append(Integer.toHexString(obj.hashCode())).toString();
  }

  /**
   * Return a String representation of an object's overall identity.
   *
   * @param obj
   *         the object (may be {@code null})
   *
   * @return the object's identity as String representation,
   * or an empty String if the object was {@code null}
   */
  public static String identityToString(Object obj) {
    if (obj == null) {
      return Constant.BLANK;
    }
    String className = obj.getClass().getName();
    String identityHexString = getIdentityHexString(obj);
    return className + '@' + identityHexString;
  }

  /**
   * Return a hex String form of an object's identity hash code.
   *
   * @param obj
   *         the object
   *
   * @return the object's identity code in hex notation
   */
  public static String getIdentityHexString(Object obj) {
    return Integer.toHexString(System.identityHashCode(obj));
  }

  /**
   * Check whether the given array contains the given element.
   *
   * @param array
   *         the array to check (may be {@code null},
   *         in which case the return value will always be {@code false})
   * @param element
   *         the element to check for
   *
   * @return whether the element has been found in the given array
   *
   * @since 3.0
   */
  public static boolean containsElement(Object[] array, Object element) {
    if (array == null) {
      return false;
    }
    for (Object arrayEle : array) {
      if (Objects.equals(arrayEle, element)) {
        return true;
      }
    }
    return false;
  }

}
