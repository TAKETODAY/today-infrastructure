/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import cn.taketoday.context.utils.NumberUtils;

import static cn.taketoday.context.Constant.BLANK;

/**
 * A helper class of Arithmetic defined by the EL Specification
 *
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public abstract class ExpressionArithmetic extends ExpressionUtils {

  public static final LongDelegate LONG = new LongDelegate();
  public static final DoubleDelegate DOUBLE = new DoubleDelegate();
  public static final BigDecimalDelegate BIGDECIMAL = new BigDecimalDelegate();
  public static final BigIntegerDelegate BIGINTEGER = new BigIntegerDelegate();

  private static final ExpressionArithmetic[] EXPRESSION_ARITHMETICS = new ExpressionArithmetic[] { //
          BIGDECIMAL, DOUBLE, BIGINTEGER
  };

  protected ExpressionArithmetic() {
    super();
  }

  public static Number add(final Object obj0, final Object obj1) {
    return operation(obj0, obj1, ExpressionArithmetic::addInternal);
  }

  public static Number mod(final Object obj0, final Object obj1) {
    return operation(obj0, obj1, ExpressionArithmetic::modInternal);
  }

  public static Number subtract(final Object obj0, final Object obj1) {
    return operation(obj0, obj1, ExpressionArithmetic::subtractInternal);
  }

  public static Number multiply(final Object obj0, final Object obj1) {
    return operation(obj0, obj1, ExpressionArithmetic::multiplyInternal);
  }

  public static Number divide(final Object obj0, final Object obj1) {
    if (obj0 == null && obj1 == null) {
      return ZERO;
    }
    final ExpressionArithmetic delegate;
    if (BIGDECIMAL.matches(obj0, obj1)) delegate = BIGDECIMAL;
    else if (BIGINTEGER.matches(obj0, obj1)) delegate = BIGDECIMAL;
    else
      delegate = DOUBLE;
    return delegate.divideInternal(delegate.convert(obj0), delegate.convert(obj1));
  }

  public static boolean isNumber(final Object obj) {
    return (obj != null && NumberUtils.isNumber(obj.getClass()));
  }

  interface Operation {
    Number apply(ExpressionArithmetic who, Number o0, Number o1);
  }

  protected static Number operation(final Object obj0, final Object obj1, final Operation operation) {
    if (obj0 == null && obj1 == null) {
      return ZERO;
    }
    for (final ExpressionArithmetic arithmetic : EXPRESSION_ARITHMETICS) {
      if (arithmetic.matches(obj0, obj1)) {
        return operation.apply(arithmetic, arithmetic.convert(obj0), arithmetic.convert(obj1));
      }
    }
    final ExpressionArithmetic arithmetic = LONG;
    return operation.apply(arithmetic, arithmetic.convert(obj0), arithmetic.convert(obj1));
  }
  //@off
  protected abstract Number convert(final Number num);
  protected abstract Number convert(final String str);

  protected abstract Number modInternal(final Number num0, final Number num1);
  protected abstract Number addInternal(final Number num0, final Number num1);
  protected abstract Number divideInternal(final Number num0, final Number num1);
  protected abstract Number multiplyInternal(final Number num0, final Number num1);
  protected abstract Number subtractInternal(final Number num0, final Number num1);
  protected abstract boolean matches(final Object obj0, final Object obj1);//@on

  protected final Number convert(final Object obj) {

    if (obj == null) {
      return convert(ZERO);
    }

    if (isNumber(obj)) {
      return convert((Number) obj);
    }

    if (obj instanceof String) {
      return BLANK.equals(obj) ? convert(ZERO) : convert((String) obj);
    }

    final Class<?> objType = obj.getClass();
    if (objType == Character.class || objType == Character.TYPE) {
      return convert(Short.valueOf((short) ((Character) obj).charValue()));
    }

    throw new IllegalArgumentException("Cannot convert " + obj + " of type " + objType + " to Number");
  }

  static final class BigDecimalDelegate extends ExpressionArithmetic {
    @Override
    protected Number addInternal(Number num0, Number num1) {
      return ((BigDecimal) num0).add((BigDecimal) num1);
    }

    @Override
    protected Number convert(Number num) {
      if (num instanceof BigDecimal) {
        return num;
      }
      return num instanceof BigInteger
             ? new BigDecimal((BigInteger) num) : BigDecimal.valueOf(num.doubleValue());
    }

    @Override
    protected Number convert(String str) {
      return new BigDecimal(str);
    }

    @Override
    protected Number divideInternal(Number num0, Number num1) {
      return ((BigDecimal) num0).divide((BigDecimal) num1, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    protected Number subtractInternal(Number num0, Number num1) {
      return ((BigDecimal) num0).subtract((BigDecimal) num1);
    }

    @Override
    protected Number modInternal(Number num0, Number num1) {
      return num0.doubleValue() % num1.doubleValue();
    }

    @Override
    protected Number multiplyInternal(Number num0, Number num1) {
      return ((BigDecimal) num0).multiply((BigDecimal) num1);
    }

    @Override
    public boolean matches(Object obj0, Object obj1) {
      return obj0 instanceof BigDecimal || obj1 instanceof BigDecimal;
    }
  }

  static class BigIntegerDelegate extends ExpressionArithmetic {
    @Override
    protected Number addInternal(Number num0, Number num1) {
      return ((BigInteger) num0).add((BigInteger) num1);
    }

    @Override
    protected Number convert(Number num) {
      return num instanceof BigInteger ? num : new BigInteger(num.toString());
    }

    @Override
    protected Number convert(String str) {
      return new BigInteger(str);
    }

    @Override
    protected Number divideInternal(Number num0, Number num1) {
      return new BigDecimal((BigInteger) num0)
              .divide(new BigDecimal((BigInteger) num1), RoundingMode.HALF_UP);
    }

    @Override
    protected Number multiplyInternal(Number num0, Number num1) {
      return ((BigInteger) num0).multiply((BigInteger) num1);
    }

    @Override
    protected Number modInternal(Number num0, Number num1) {
      return ((BigInteger) num0).mod((BigInteger) num1);
    }

    @Override
    protected Number subtractInternal(Number num0, Number num1) {
      return ((BigInteger) num0).subtract((BigInteger) num1);
    }

    @Override
    public boolean matches(Object obj0, Object obj1) {
      return obj0 instanceof BigInteger || obj1 instanceof BigInteger;
    }
  }

  static class DoubleDelegate extends ExpressionArithmetic {
    @Override
    protected Number addInternal(Number num0, Number num1) {
      // could only be one of these
      if (num0 instanceof BigDecimal) {
        return ((BigDecimal) num0).add(BigDecimal.valueOf(num1.doubleValue()));
      }
      if (num1 instanceof BigDecimal) {
        return BigDecimal.valueOf(num0.doubleValue()).add((BigDecimal) num1);
      }
      return num0.doubleValue() + num1.doubleValue();
    }

    @Override
    protected Number convert(Number num) {
      if (num instanceof Double) return num;
      if (num instanceof BigInteger) return new BigDecimal((BigInteger) num);
      return num.doubleValue();
    }

    @Override
    protected Number convert(String str) {
      return Double.parseDouble(str);
    }

    @Override
    protected Number divideInternal(Number num0, Number num1) {
      return num0.doubleValue() / num1.doubleValue();
    }

    @Override
    protected Number modInternal(Number num0, Number num1) {
      return num0.doubleValue() % num1.doubleValue();
    }

    @Override
    protected Number subtractInternal(Number num0, Number num1) {
      // could only be one of these
      if (num0 instanceof BigDecimal) {
        return ((BigDecimal) num0).subtract(BigDecimal.valueOf(num1.doubleValue()));
      }
      else if (num1 instanceof BigDecimal) {
        return ((BigDecimal.valueOf(num0.doubleValue()).subtract((BigDecimal) num1)));
      }
      return num0.doubleValue() - num1.doubleValue();
    }

    @Override
    protected Number multiplyInternal(Number num0, Number num1) {
      // could only be one of these
      if (num0 instanceof BigDecimal) {
        return ((BigDecimal) num0).multiply(BigDecimal.valueOf(num1.doubleValue()));
      }
      else if (num1 instanceof BigDecimal) {
        return ((BigDecimal.valueOf(num0.doubleValue()).multiply((BigDecimal) num1)));
      }
      return num0.doubleValue() * num1.doubleValue();
    }

    @Override
    public boolean matches(Object obj0, Object obj1) {
      return (obj0 instanceof Double
              || obj1 instanceof Double
              || obj0 instanceof Float
              || obj1 instanceof Float
              || (obj0 != null && (Double.TYPE == obj0.getClass() || Float.TYPE == obj0.getClass()))
              || (obj1 != null && (Double.TYPE == obj1.getClass() || Float.TYPE == obj1.getClass()))
              || (obj0 instanceof String && ExpressionUtils.isStringFloat((String) obj0))
              || (obj1 instanceof String && ExpressionUtils.isStringFloat((String) obj1))//
      );
    }
  }

  public static final class LongDelegate extends ExpressionArithmetic {
    @Override
    protected Number addInternal(Number num0, Number num1) {
      return num0.longValue() + num1.longValue();
    }

    @Override
    protected Number convert(Number num) {
      // if (num instanceof Long)
      //   return num;
      return num.longValue();
    }

    @Override
    protected Number convert(String str) {
      return Long.parseLong(str);
    }

    @Override
    protected Number divideInternal(Number num0, Number num1) {
      return num0.longValue() / num1.longValue();
    }

    @Override
    protected Number modInternal(Number num0, Number num1) {
      return num0.longValue() % num1.longValue();
    }

    @Override
    protected Number subtractInternal(Number num0, Number num1) {
      return num0.longValue() - num1.longValue();
    }

    @Override
    protected Number multiplyInternal(Number num0, Number num1) {
      return num0.longValue() * num1.longValue();
    }

    @Override
    public boolean matches(Object obj0, Object obj1) {
      return (obj0 instanceof Long || obj1 instanceof Long);
    }
  }

}
