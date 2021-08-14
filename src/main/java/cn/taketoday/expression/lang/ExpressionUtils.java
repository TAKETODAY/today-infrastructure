/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package cn.taketoday.expression.lang;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.math.BigDecimal;
import java.math.BigInteger;

import cn.taketoday.core.utils.NumberUtils;
import cn.taketoday.core.utils.StringUtils;
import cn.taketoday.expression.ExpressionException;

import static cn.taketoday.core.Constant.BLANK;

/**
 * A helper class that implements the EL Specification
 *
 * @author Jacob Hookom [jacob@hookom.net]
 * @author Kin-man Chung
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public abstract class ExpressionUtils {

  protected static final Long ZERO = 0L;

  /**
   * @param obj0
   *         First object to be compared
   * @param obj1
   *         Second object to be compared
   *
   * @return The result (an int with values -1, 0, or 1) of the comparison
   *
   * @throws ExpressionException
   */
  @SuppressWarnings("unchecked")
  public static int compare(final Object obj0, final Object obj1) throws ExpressionException {
    if (equals(obj0, obj1)) {
      return 0;
    }
    if (isLongOp(obj0, obj1)) {
      final Long l0 = (Long) coerceToNumber(obj0, Long.class);
      final Long l1 = (Long) coerceToNumber(obj1, Long.class);
      return l0.compareTo(l1);
    }
    if (isDoubleOp(obj0, obj1)) {
      final Double d0 = (Double) coerceToNumber(obj0, Double.class);
      final Double d1 = (Double) coerceToNumber(obj1, Double.class);
      return d0.compareTo(d1);
    }
    if (isBigIntegerOp(obj0, obj1)) {
      final BigInteger bi0 = (BigInteger) coerceToNumber(obj0, BigInteger.class);
      final BigInteger bi1 = (BigInteger) coerceToNumber(obj1, BigInteger.class);
      return bi0.compareTo(bi1);
    }

    if (isBigDecimalOp(obj0, obj1)) {
      final BigDecimal bd0 = (BigDecimal) coerceToNumber(obj0, BigDecimal.class);
      final BigDecimal bd1 = (BigDecimal) coerceToNumber(obj1, BigDecimal.class);
      return bd0.compareTo(bd1);
    }
    if (obj0 instanceof String || obj1 instanceof String) {
      return coerceToString(obj0).compareTo(coerceToString(obj1));
    }
    if (obj0 instanceof Comparable) {
      return (obj1 != null) ? ((Comparable<Object>) obj0).compareTo(obj1) : 1;
    }
    if (obj1 instanceof Comparable) {
      return (obj0 != null) ? -(((Comparable<Object>) obj1).compareTo(obj0)) : -1;
    }
    throw new ExpressionException("Cannot compare " + obj0 + " to " + obj1);
  }

  /**
   * @param obj0
   *         Fisrt object to be compared
   * @param obj1
   *         Second object to be compared
   *
   * @return true if the objects compared equal
   *
   * @throws ExpressionException
   */
  public static boolean equals(final Object obj0, final Object obj1) throws ExpressionException {
    if (obj0 == obj1) {
      return true;
    }
    if (obj0 == null || obj1 == null) {
      return false;
    }
    if (obj0 instanceof Boolean || obj1 instanceof Boolean) {
      return coerceToBoolean(obj0).equals(coerceToBoolean(obj1));
    }
    if (obj0.getClass().isEnum()) {
      return obj0.equals(coerceToEnum(obj1, obj0.getClass()));
    }
    if (obj1.getClass().isEnum()) {
      return obj1.equals(coerceToEnum(obj0, obj1.getClass()));
    }
    if (obj0 instanceof String || obj1 instanceof String) {
      return coerceToString(obj0).equals(coerceToString(obj1));
    }
    if (isBigDecimalOp(obj0, obj1)) {
      return coerceToNumber(obj0, BigDecimal.class).equals(coerceToNumber(obj1, BigDecimal.class));
    }
    if (isDoubleOp(obj0, obj1)) {
      return coerceToNumber(obj0, Double.class).equals(coerceToNumber(obj1, Double.class));
    }
    if (isBigIntegerOp(obj0, obj1)) {
      return coerceToNumber(obj0, BigInteger.class).equals(coerceToNumber(obj1, BigInteger.class));
    }
    if (isLongOp(obj0, obj1)) {
      return coerceToNumber(obj0, Long.class).equals(coerceToNumber(obj1, Long.class));
    }
    else {
      return obj0.equals(obj1);
    }
  }

  /**
   * @param obj
   *         Object to be coerced
   *
   * @return The result of coercion
   */
  public static Boolean coerceToBoolean(final Object obj) throws IllegalArgumentException {

    if (obj == null || BLANK.equals(obj)) {
      return Boolean.FALSE;
    }
    if (obj instanceof Boolean) {
      return (Boolean) obj;
    }
    if (obj instanceof String) {
      return Boolean.valueOf((String) obj);
    }

    throw new IllegalArgumentException("Cannot convert " + obj + " of type " + obj.getClass() + " to Boolean");
  }

  // Enum types are hard construct. We can declare this as
  // <T extends Enum<T>> T coerceToEnum(Object, Class<T> type)
  // but this makes it harder to get the calls right.
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static Enum coerceToEnum(final Object obj, Class type) {

    if (obj == null || BLANK.equals(obj)) {
      return null;
    }
    if (obj.getClass().isEnum()) {
      return (Enum) obj;
    }
    return Enum.valueOf(type, obj.toString());
  }

  public static Character coerceToCharacter(final Object obj) throws IllegalArgumentException {

    if (obj == null || BLANK.equals(obj)) {
      return (char) 0;
    }
    if (obj instanceof String) {
      return ((String) obj).charAt(0);
    }
    if (ExpressionArithmetic.isNumber(obj)) {
      return (char) ((Number) obj).shortValue();
    }
    if (obj instanceof Character) {
      return (Character) obj;
    }
    throw new IllegalArgumentException("Cannot convert " + obj + " of type " + obj.getClass() + " to Character");
  }

  public static Number coerceToNumber(final Object obj) {
    if (obj == null) {
      return ZERO;
    }
    if (obj instanceof Number) {
      return (Number) obj;
    }
    String str = coerceToString(obj);
    if (isStringFloat(str)) {
      return toFloat(str);
    }
    return toNumber(str);
  }

  protected static Number coerceToNumber(final Number number, final Class<?> type) throws IllegalArgumentException {

    if (Long.TYPE == type || Long.class == type) {
      return number.longValue();
    }

    if (Double.TYPE == type || Double.class == type) {
      return number.doubleValue();
    }

    if (Integer.TYPE == type || Integer.class == type) {
      return number.intValue();
    }
    if (BigInteger.class == type) {
      if (number instanceof BigDecimal) {
        return ((BigDecimal) number).toBigInteger();
      }
      if (number instanceof BigInteger) {
        return number;
      }
      return BigInteger.valueOf(number.longValue());
    }
    if (BigDecimal.class == type) {
      if (number instanceof BigDecimal) {
        return number;
      }
      if (number instanceof BigInteger) {
        return new BigDecimal((BigInteger) number);
      }
      if (number instanceof Long) {
        return new BigDecimal((Long) number);
      }
      return BigDecimal.valueOf(number.doubleValue());
    }
    if (Byte.TYPE == type || Byte.class.equals(type)) {
      return number.byteValue();
    }
    if (Short.TYPE == type || Short.class.equals(type)) {
      return number.shortValue();
    }
    if (Float.TYPE == type || Float.class.equals(type)) {
      return number.floatValue();
    }

    throw new IllegalArgumentException("Cannot convert " + number + " of type " + number.getClass() + " to " + type);
  }

  public static Number coerceToNumber(final Object obj, final Class<?> type) throws IllegalArgumentException {

    if (obj == null) {
      return coerceToNumber(ZERO, type);
    }
    if (obj.getClass() == type) {
      return (Number) obj;
    }
    if (obj instanceof String) {
      return coerceToNumber((String) obj, type);
    }
    if (ExpressionArithmetic.isNumber(obj)) {
      return coerceToNumber((Number) obj, type);
    }
    if (obj instanceof Character) {
      return coerceToNumber((short) ((Character) obj).charValue(), type);
    }
    throw new IllegalArgumentException("Cannot convert " + obj + " of type " + obj.getClass() + " to " + type);
  }

  protected static Number coerceToNumber(final String val, final Class<?> type) throws IllegalArgumentException {
    if (StringUtils.isEmpty(val)) {
      return coerceToNumber(ZERO, type);
    }
    if (long.class == type || Long.class == type) {
      return Long.valueOf(val);
    }
    if (int.class == type || Integer.class == type) {
      return Integer.valueOf(val);
    }
    if (double.class == type || Double.class == type) {
      return Double.valueOf(val);
    }
    if (BigInteger.class == type) {
      return new BigInteger(val);
    }
    if (BigDecimal.class == type) {
      return new BigDecimal(val);
    }
    if (byte.class == type || Byte.class == type) {
      return Byte.valueOf(val);
    }
    if (short.class == type || Short.class == type) {
      return Short.valueOf(val);
    }
    if (float.class == type || Float.class == type) {
      return Float.valueOf(val);
    }

    throw new IllegalArgumentException("Cannot convert " + val + " of type " + String.class + " to " + type);
  }

  /**
   * @param obj
   *         Object to be coerced
   *
   * @return The result of coercion
   */
  public static String coerceToString(final Object obj) {
    if (obj == null) {
      return BLANK;
    }
    if (obj instanceof String) {
      return (String) obj;
    }
    if (obj instanceof Enum) {
      return ((Enum<?>) obj).name();
    }
    return obj.toString();
  }

  public static Object coerceToType(final Object obj, final Class<?> type) throws IllegalArgumentException {

    if (type == null || type.isInstance(obj)) {
      return obj;
    }

    if (obj == null && !type.isPrimitive() && String.class != type) {
      return null;
    }

    if (String.class == type) {
      return coerceToString(obj);
    }
    if (NumberUtils.isNumber(type)) {
      return coerceToNumber(obj, type);
    }
    if (Character.class == type || char.class == type) {
      return coerceToCharacter(obj);
    }
    if (Boolean.class == type || boolean.class == type) {
      return coerceToBoolean(obj);
    }
    if (type.isEnum()) {
      return coerceToEnum(obj, type);
    }

    if (obj instanceof String) {
      if (BLANK.equals(obj)) {
        return null;
      }
      PropertyEditor editor = PropertyEditorManager.findEditor(type);
      if (editor != null) {
        editor.setAsText((String) obj);
        return editor.getValue();
      }
    }
    throw new IllegalArgumentException("Cannot convert " + obj + " of type " + obj.getClass() + " to " + type);
  }

  //    /**
  //     * @param obj
  //     *            An array of objects
  //     * @return true if the array contains a null, false otherwise
  //     */
  //    public static boolean containsNulls(final Object[] obj) {
  //        for (int i = 0; i < obj.length; i++) {
  //            if (obj[0] == null) {
  //                return true;
  //            }
  //        }
  //        return false;
  //    }

  public static boolean isBigDecimalOp(final Object obj0, final Object obj1) {
    return (obj0 instanceof BigDecimal || obj1 instanceof BigDecimal);
  }

  public static boolean isBigIntegerOp(final Object obj0, final Object obj1) {
    return (obj0 instanceof BigInteger || obj1 instanceof BigInteger);
  }

  public static boolean isDoubleOp(final Object obj0, final Object obj1) {
    return (obj0 instanceof Double || obj1 instanceof Double || obj0 instanceof Float || obj1 instanceof Float);
  }

  public static boolean isDoubleStringOp(final Object obj0, final Object obj1) {
    return (isDoubleOp(obj0, obj1) //
            || (obj0 instanceof String && isStringFloat((String) obj0)) //
            || (obj1 instanceof String && isStringFloat((String) obj1))//
    );
  }

  public static boolean isLongOp(final Object obj0, final Object obj1) {
    return (obj0 instanceof Long || obj1 instanceof Long || obj0 instanceof Integer || obj1 instanceof Integer
            || obj0 instanceof Character || obj1 instanceof Character || obj0 instanceof Short || obj1 instanceof Short
            || obj0 instanceof Byte || obj1 instanceof Byte);
  }

  public static boolean isStringFloat(final String str) {
    final int len = str.length();
    if (len > 1) {
      for (int i = 0; i < len; i++) {
        switch (str.charAt(i)) {
          case 'E':
          case 'e':
          case '.':
            return true;
        }
      }
    }
    return false;
  }

  public static Number toFloat(final String value) {
    try {

      final double parseDouble = Double.parseDouble(value);
      if (parseDouble > Double.MAX_VALUE) {
        return new BigDecimal(value);
      }
      return parseDouble;
    }
    catch (NumberFormatException e0) {
      return new BigDecimal(value);
    }
  }

  public static Number toNumber(final String value) {
    try {
      return Integer.valueOf(value);
    }
    catch (NumberFormatException e0) {
      try {
        return Long.valueOf(value);
      }
      catch (NumberFormatException e1) {
        return new BigInteger(value);
      }
    }
  }

}
