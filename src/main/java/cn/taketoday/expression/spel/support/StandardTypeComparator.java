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

package cn.taketoday.expression.spel.support;

import java.math.BigDecimal;
import java.math.BigInteger;

import cn.taketoday.expression.TypeComparator;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.NumberUtils;

/**
 * A basic {@link TypeComparator} implementation: supports comparison of
 * {@link Number} types as well as types implementing {@link Comparable}.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @since 4.0
 */
public class StandardTypeComparator implements TypeComparator {

  @Override
  public boolean canCompare(@Nullable Object left, @Nullable Object right) {
    if (left == null || right == null) {
      return true;
    }
    if (left instanceof Number && right instanceof Number) {
      return true;
    }
    if (left instanceof Comparable && right instanceof Comparable) {
      Class<?> ancestor = ClassUtils.determineCommonAncestor(left.getClass(), right.getClass());
      return ancestor != null && Comparable.class.isAssignableFrom(ancestor);
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public int compare(@Nullable Object left, @Nullable Object right) throws SpelEvaluationException {
    // If one is null, check if the other is
    if (left == null) {
      return (right == null ? 0 : -1);
    }
    else if (right == null) {
      return 1;  // left cannot be null at this point
    }

    // Basic number comparisons
    if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
      if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
        BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
        BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
        return leftBigDecimal.compareTo(rightBigDecimal);
      }
      else if (leftNumber instanceof Double || rightNumber instanceof Double) {
        return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
      }
      else if (leftNumber instanceof Float || rightNumber instanceof Float) {
        return Float.compare(leftNumber.floatValue(), rightNumber.floatValue());
      }
      else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
        BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
        BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
        return leftBigInteger.compareTo(rightBigInteger);
      }
      else if (leftNumber instanceof Long || rightNumber instanceof Long) {
        return Long.compare(leftNumber.longValue(), rightNumber.longValue());
      }
      else if (leftNumber instanceof Integer || rightNumber instanceof Integer) {
        return Integer.compare(leftNumber.intValue(), rightNumber.intValue());
      }
      else if (leftNumber instanceof Short || rightNumber instanceof Short) {
        return Short.compare(leftNumber.shortValue(), rightNumber.shortValue());
      }
      else if (leftNumber instanceof Byte || rightNumber instanceof Byte) {
        return Byte.compare(leftNumber.byteValue(), rightNumber.byteValue());
      }
      else {
        // Unknown Number subtype -> best guess is double multiplication
        return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
      }
    }

    try {
      if (left instanceof Comparable) {
        return ((Comparable<Object>) left).compareTo(right);
      }
    }
    catch (ClassCastException ex) {
      throw new SpelEvaluationException(ex, SpelMessage.NOT_COMPARABLE, left.getClass(), right.getClass());
    }

    throw new SpelEvaluationException(SpelMessage.NOT_COMPARABLE, left.getClass(), right.getClass());
  }

}
