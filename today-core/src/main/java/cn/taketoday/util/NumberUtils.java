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
package cn.taketoday.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Miscellaneous utility methods for number conversion and parsing.
 * <p>Mainly for internal use within the framework; consider Apache's
 * Commons Lang for a more comprehensive suite of number utilities.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-07-06 13:36:29
 */
public abstract class NumberUtils {

  private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
  private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

  /**
   * Standard number types (all immutable):
   * Byte, Short, Integer, Long, BigInteger, Float, Double, BigDecimal.
   */
  public static final Set<Class<?>> STANDARD_NUMBER_TYPES = Set.of(
          Byte.class, Short.class, Integer.class, Long.class,
          BigInteger.class, Float.class, Double.class, BigDecimal.class
  );

  /**
   * Is a number?
   *
   * @param targetClass the target class
   */
  public static boolean isNumber(Class<?> targetClass) {
    return Number.class.isAssignableFrom(targetClass)
            || targetClass == int.class
            || targetClass == long.class
            || targetClass == float.class
            || targetClass == double.class
            || targetClass == short.class
            || targetClass == byte.class;
  }

  /**
   * Convert the given number into an instance of the given target class.
   *
   * @param number the number to convert
   * @param targetClass the target class to convert to
   * @return the converted number
   * @throws IllegalArgumentException if the target class is not supported
   * (i.e. not a standard Number subclass as included in the JDK)
   * @see java.lang.Byte
   * @see java.lang.Short
   * @see java.lang.Integer
   * @see java.lang.Long
   * @see java.math.BigInteger
   * @see java.lang.Float
   * @see java.lang.Double
   * @see java.math.BigDecimal
   */
  @SuppressWarnings("unchecked")
  public static <T extends Number> T convertNumberToTargetClass(Number number, Class<T> targetClass)
          throws IllegalArgumentException {

    Assert.notNull(number, "Number is required");
    Assert.notNull(targetClass, "Target class is required");

    if (targetClass.isInstance(number)) {
      return (T) number;
    }
    else if (Byte.class == targetClass) {
      long value = checkedLongValue(number, targetClass);
      if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
        raiseOverflowException(number, targetClass);
      }
      return (T) Byte.valueOf(number.byteValue());
    }
    else if (Short.class == targetClass) {
      long value = checkedLongValue(number, targetClass);
      if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
        raiseOverflowException(number, targetClass);
      }
      return (T) Short.valueOf(number.shortValue());
    }
    else if (Integer.class == targetClass) {
      long value = checkedLongValue(number, targetClass);
      if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
        raiseOverflowException(number, targetClass);
      }
      return (T) Integer.valueOf(number.intValue());
    }
    else if (Long.class == targetClass) {
      long value = checkedLongValue(number, targetClass);
      return (T) Long.valueOf(value);
    }
    else if (BigInteger.class == targetClass) {
      if (number instanceof BigDecimal) {
        // do not lose precision - use BigDecimal's own conversion
        return (T) ((BigDecimal) number).toBigInteger();
      }
      else {
        // original value is not a Big* number - use standard long conversion
        return (T) BigInteger.valueOf(number.longValue());
      }
    }
    else if (Float.class == targetClass) {
      return (T) Float.valueOf(number.floatValue());
    }
    else if (Double.class == targetClass) {
      return (T) Double.valueOf(number.doubleValue());
    }
    else if (BigDecimal.class == targetClass) {
      // always use BigDecimal(String) here to avoid unpredictability of BigDecimal(double)
      // (see BigDecimal javadoc for details)
      return (T) new BigDecimal(number.toString());
    }
    else {
      throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" +
              number.getClass().getName() + "] to unsupported target class [" + targetClass.getName() + "]");
    }
  }

  /**
   * Check for a {@code BigInteger}/{@code BigDecimal} long overflow
   * before returning the given number as a long value.
   *
   * @param number the number to convert
   * @param targetClass the target class to convert to
   * @return the long value, if convertible without overflow
   * @throws IllegalArgumentException if there is an overflow
   * @see #raiseOverflowException
   */
  private static long checkedLongValue(Number number, Class<? extends Number> targetClass) {
    BigInteger bigInt = null;
    if (number instanceof BigInteger) {
      bigInt = (BigInteger) number;
    }
    else if (number instanceof BigDecimal) {
      bigInt = ((BigDecimal) number).toBigInteger();
    }
    // Effectively analogous to JDK 8's BigInteger.longValueExact()
    if (bigInt != null && (bigInt.compareTo(LONG_MIN) < 0 || bigInt.compareTo(LONG_MAX) > 0)) {
      raiseOverflowException(number, targetClass);
    }
    return number.longValue();
  }

  /**
   * Raise an <em>overflow</em> exception for the given number and target class.
   *
   * @param number the number we tried to convert
   * @param targetClass the target class we tried to convert to
   * @throws IllegalArgumentException if there is an overflow
   */
  private static void raiseOverflowException(Number number, Class<?> targetClass) {
    throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" +
            number.getClass().getName() + "] to target class [" + targetClass.getName() + "]: overflow");
  }

  /**
   * Parse the given {@code text} into a {@link Number} instance of the given
   * target class, using the corresponding {@code decode} / {@code valueOf} method.
   * <p>Trims all whitespace (leading, trailing, and in between characters) from
   * the input {@code String} before attempting to parse the number.
   * <p>Supports numbers in hex format (with leading "0x", "0X", or "#") as well.
   *
   * @param text the text to convert
   * @param targetClass the target class to parse into
   * @return the parsed number
   * @throws IllegalArgumentException if the target class is not supported
   * (i.e. not a standard Number subclass as included in the JDK)
   * @see Byte#decode
   * @see Short#decode
   * @see Integer#decode
   * @see Long#decode
   * @see #decodeBigInteger(String)
   * @see Float#valueOf
   * @see Double#valueOf
   * @see java.math.BigDecimal#BigDecimal(String)
   */
  @SuppressWarnings("unchecked")
  public static <T extends Number> T parseNumber(String text, Class<T> targetClass) {
    Assert.notNull(text, "Text is required");
    Assert.notNull(targetClass, "Target class is required");
    String trimmed = StringUtils.trimAllWhitespace(text);

    if (Byte.class == targetClass) {
      return (T) (isHexNumber(trimmed) ? Byte.decode(trimmed) : Byte.valueOf(trimmed));
    }
    else if (Short.class == targetClass) {
      return (T) (isHexNumber(trimmed) ? Short.decode(trimmed) : Short.valueOf(trimmed));
    }
    else if (Integer.class == targetClass) {
      return (T) (isHexNumber(trimmed) ? Integer.decode(trimmed) : Integer.valueOf(trimmed));
    }
    else if (Long.class == targetClass) {
      return (T) (isHexNumber(trimmed) ? Long.decode(trimmed) : Long.valueOf(trimmed));
    }
    else if (BigInteger.class == targetClass) {
      return (T) (isHexNumber(trimmed) ? decodeBigInteger(trimmed) : new BigInteger(trimmed));
    }
    else if (Float.class == targetClass) {
      return (T) Float.valueOf(trimmed);
    }
    else if (Double.class == targetClass) {
      return (T) Double.valueOf(trimmed);
    }
    else if (BigDecimal.class == targetClass || Number.class == targetClass) {
      return (T) new BigDecimal(trimmed);
    }
    else {
      throw new IllegalArgumentException(
              "Cannot convert String [" + text + "] to target class [" + targetClass.getName() + "]");
    }
  }

  /**
   * Parse the given {@code text} into a {@link Number} instance of the
   * given target class, using the supplied {@link NumberFormat}.
   * <p>Trims the input {@code String} before attempting to parse the number.
   *
   * @param text the text to convert
   * @param targetClass the target class to parse into
   * @param numberFormat the {@code NumberFormat} to use for parsing (if
   * {@code null}, this method falls back to {@link #parseNumber(String, Class)})
   * @return the parsed number
   * @throws IllegalArgumentException if the target class is not supported
   * (i.e. not a standard Number subclass as included in the JDK)
   * @see java.text.NumberFormat#parse
   * @see #convertNumberToTargetClass
   * @see #parseNumber(String, Class)
   */
  public static <T extends Number> T parseNumber(
          String text, Class<T> targetClass, @Nullable NumberFormat numberFormat) {

    if (numberFormat != null) {
      Assert.notNull(text, "Text is required");
      Assert.notNull(targetClass, "Target class is required");
      DecimalFormat decimalFormat = null;
      boolean resetBigDecimal = false;
      if (numberFormat instanceof DecimalFormat) {
        decimalFormat = (DecimalFormat) numberFormat;
        if (BigDecimal.class == targetClass && !decimalFormat.isParseBigDecimal()) {
          decimalFormat.setParseBigDecimal(true);
          resetBigDecimal = true;
        }
      }
      try {
        Number number = numberFormat.parse(StringUtils.trimAllWhitespace(text));
        return convertNumberToTargetClass(number, targetClass);
      }
      catch (ParseException ex) {
        throw new IllegalArgumentException("Could not parse number: " + ex.getMessage());
      }
      finally {
        if (resetBigDecimal) {
          decimalFormat.setParseBigDecimal(false);
        }
      }
    }
    else {
      return parseNumber(text, targetClass);
    }
  }

  /**
   * Determine whether the given {@code value} String indicates a hex number,
   * i.e. needs to be passed into {@code Integer.decode} instead of
   * {@code Integer.valueOf}, etc.
   */
  private static boolean isHexNumber(String value) {
    int index = (value.startsWith("-") ? 1 : 0);
    return (value.startsWith("0x", index) || value.startsWith("0X", index) || value.startsWith("#", index));
  }

  /**
   * Decode a {@link java.math.BigInteger} from the supplied {@link String} value.
   * <p>Supports decimal, hex, and octal notation.
   *
   * @see BigInteger#BigInteger(String, int)
   */
  private static BigInteger decodeBigInteger(String value) {
    int radix = 10;
    int index = 0;
    boolean negative = false;

    // Handle minus sign, if present.
    if (value.startsWith("-")) {
      negative = true;
      index++;
    }

    // Handle radix specifier, if present.
    if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
      index += 2;
      radix = 16;
    }
    else if (value.startsWith("#", index)) {
      index++;
      radix = 16;
    }
    else if (value.startsWith("0", index) && value.length() > 1 + index) {
      index++;
      radix = 8;
    }

    BigInteger result = new BigInteger(value.substring(index), radix);
    return (negative ? result.negate() : result);
  }

}
