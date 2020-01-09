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

import static cn.taketoday.context.Constant.BLANK;

import java.math.BigDecimal;
import java.math.BigInteger;

import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.expression.util.MessageFactory;

/**
 * A helper class of Arithmetic defined by the EL Specification
 * 
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public abstract class ExpressionArithmetic {

    private final static Long ZERO = Long.valueOf(0);
    public final static LongDelegate LONG = new LongDelegate();
    public final static DoubleDelegate DOUBLE = new DoubleDelegate();
    public final static BigDecimalDelegate BIGDECIMAL = new BigDecimalDelegate();
    public final static BigIntegerDelegate BIGINTEGER = new BigIntegerDelegate();

    public final static class BigDecimalDelegate extends ExpressionArithmetic {
        @Override
        protected Number add(Number num0, Number num1) {
            return ((BigDecimal) num0).add((BigDecimal) num1);
        }

        @Override
        protected Number convert(Number num) {
            if (num instanceof BigDecimal) return num;
            if (num instanceof BigInteger) return new BigDecimal((BigInteger) num);
            return new BigDecimal(num.doubleValue());
        }

        @Override
        protected Number convert(String str) {
            return new BigDecimal(str);
        }

        @Override
        protected Number divide(Number num0, Number num1) {
            return ((BigDecimal) num0).divide((BigDecimal) num1, BigDecimal.ROUND_HALF_UP);
        }

        @Override
        protected Number subtract(Number num0, Number num1) {
            return ((BigDecimal) num0).subtract((BigDecimal) num1);
        }

        @Override
        protected Number mod(Number num0, Number num1) {
            return num0.doubleValue() % num1.doubleValue();
        }

        @Override
        protected Number multiply(Number num0, Number num1) {
            return ((BigDecimal) num0).multiply((BigDecimal) num1);
        }

        @Override
        public boolean matches(Object obj0, Object obj1) {
            return (obj0 instanceof BigDecimal || obj1 instanceof BigDecimal);
        }
    }

    public final static class BigIntegerDelegate extends ExpressionArithmetic {
        @Override
        protected Number add(Number num0, Number num1) {
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
        protected Number divide(Number num0, Number num1) {
            return new BigDecimal((BigInteger) num0).divide(new BigDecimal((BigInteger) num1), BigDecimal.ROUND_HALF_UP);
        }

        @Override
        protected Number multiply(Number num0, Number num1) {
            return ((BigInteger) num0).multiply((BigInteger) num1);
        }

        @Override
        protected Number mod(Number num0, Number num1) {
            return ((BigInteger) num0).mod((BigInteger) num1);
        }

        @Override
        protected Number subtract(Number num0, Number num1) {
            return ((BigInteger) num0).subtract((BigInteger) num1);
        }

        @Override
        public boolean matches(Object obj0, Object obj1) {
            return (obj0 instanceof BigInteger || obj1 instanceof BigInteger);
        }
    }

    public final static class DoubleDelegate extends ExpressionArithmetic {
        @Override
        protected Number add(Number num0, Number num1) {
            // could only be one of these
            if (num0 instanceof BigDecimal) {
                return ((BigDecimal) num0).add(new BigDecimal(num1.doubleValue()));
            }
            else if (num1 instanceof BigDecimal) {
                return new BigDecimal(num0.doubleValue()).add((BigDecimal) num1);
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
        protected Number divide(Number num0, Number num1) {
            return num0.doubleValue() / num1.doubleValue();
        }

        @Override
        protected Number mod(Number num0, Number num1) {
            return num0.doubleValue() % num1.doubleValue();
        }

        @Override
        protected Number subtract(Number num0, Number num1) {
            // could only be one of these
            if (num0 instanceof BigDecimal) {
                return ((BigDecimal) num0).subtract(new BigDecimal(num1.doubleValue()));
            }
            else if (num1 instanceof BigDecimal) {
                return ((new BigDecimal(num0.doubleValue()).subtract((BigDecimal) num1)));
            }
            return num0.doubleValue() - num1.doubleValue();
        }

        @Override
        protected Number multiply(Number num0, Number num1) {
            // could only be one of these
            if (num0 instanceof BigDecimal) {
                return ((BigDecimal) num0).multiply(new BigDecimal(num1.doubleValue()));
            }
            else if (num1 instanceof BigDecimal) {
                return ((new BigDecimal(num0.doubleValue()).multiply((BigDecimal) num1)));
            }
            return num0.doubleValue() * num1.doubleValue();
        }

        @Override
        public boolean matches(Object obj0, Object obj1) {
            return (obj0 instanceof Double //
                    || obj1 instanceof Double//
                    || obj0 instanceof Float//
                    || obj1 instanceof Float //
                    || (obj0 != null && (Double.TYPE == obj0.getClass() || Float.TYPE == obj0.getClass())) //
                    || (obj1 != null && (Double.TYPE == obj1.getClass() || Float.TYPE == obj1.getClass())) //
                    || (obj0 instanceof String && ExpressionSupport.isStringFloat((String) obj0)) //
                    || (obj1 instanceof String && ExpressionSupport.isStringFloat((String) obj1))//
            );
        }
    }

    public final static class LongDelegate extends ExpressionArithmetic {
        @Override
        protected Number add(Number num0, Number num1) {
            return Long.valueOf(num0.longValue() + num1.longValue());
        }

        @Override
        protected Number convert(Number num) {
//			if (num instanceof Long)
//				return num;
            return num.longValue();
        }

        @Override
        protected Number convert(String str) {
            return Long.parseLong(str);
        }

        @Override
        protected Number divide(Number num0, Number num1) {
            return num0.longValue() / num1.longValue();
        }

        @Override
        protected Number mod(Number num0, Number num1) {
            return num0.longValue() % num1.longValue();
        }

        @Override
        protected Number subtract(Number num0, Number num1) {
            return num0.longValue() - num1.longValue();
        }

        @Override
        protected Number multiply(Number num0, Number num1) {
            return num0.longValue() * num1.longValue();
        }

        @Override
        public boolean matches(Object obj0, Object obj1) {
            return (obj0 instanceof Long || obj1 instanceof Long);
        }
    }

    public final static Number add(final Object obj0, final Object obj1) {

        if (obj0 == null && obj1 == null) {
            return ZERO;
        }

        final ExpressionArithmetic delegate;
        if (BIGDECIMAL.matches(obj0, obj1)) delegate = BIGDECIMAL;
        else if (DOUBLE.matches(obj0, obj1)) delegate = DOUBLE;
        else if (BIGINTEGER.matches(obj0, obj1)) delegate = BIGINTEGER;
        else
            delegate = LONG;

        Number num0 = delegate.convert(obj0);
        Number num1 = delegate.convert(obj1);

        return delegate.add(num0, num1);
    }

    public final static Number mod(final Object obj0, final Object obj1) {
        if (obj0 == null && obj1 == null) {
            return Long.valueOf(0);
        }

        final ExpressionArithmetic delegate;
        if (BIGDECIMAL.matches(obj0, obj1)) delegate = BIGDECIMAL;
        else if (DOUBLE.matches(obj0, obj1)) delegate = DOUBLE;
        else if (BIGINTEGER.matches(obj0, obj1)) delegate = BIGINTEGER;
        else
            delegate = LONG;

        Number num0 = delegate.convert(obj0);
        Number num1 = delegate.convert(obj1);

        return delegate.mod(num0, num1);
    }

    public final static Number subtract(final Object obj0, final Object obj1) {
        if (obj0 == null && obj1 == null) {
            return Long.valueOf(0);
        }

        final ExpressionArithmetic delegate;
        if (BIGDECIMAL.matches(obj0, obj1)) delegate = BIGDECIMAL;
        else if (DOUBLE.matches(obj0, obj1)) delegate = DOUBLE;
        else if (BIGINTEGER.matches(obj0, obj1)) delegate = BIGINTEGER;
        else
            delegate = LONG;

        Number num0 = delegate.convert(obj0);
        Number num1 = delegate.convert(obj1);

        return delegate.subtract(num0, num1);
    }

    public final static Number divide(final Object obj0, final Object obj1) {
        if (obj0 == null && obj1 == null) {
            return ZERO;
        }

        final ExpressionArithmetic delegate;
        if (BIGDECIMAL.matches(obj0, obj1)) delegate = BIGDECIMAL;
        else if (BIGINTEGER.matches(obj0, obj1)) delegate = BIGDECIMAL;
        else
            delegate = DOUBLE;

        Number num0 = delegate.convert(obj0);
        Number num1 = delegate.convert(obj1);

        return delegate.divide(num0, num1);
    }

    public final static Number multiply(final Object obj0, final Object obj1) {
        if (obj0 == null && obj1 == null) {
            return Long.valueOf(0);
        }

        final ExpressionArithmetic delegate;
        if (BIGDECIMAL.matches(obj0, obj1)) delegate = BIGDECIMAL;
        else if (DOUBLE.matches(obj0, obj1)) delegate = DOUBLE;
        else if (BIGINTEGER.matches(obj0, obj1)) delegate = BIGINTEGER;
        else
            delegate = LONG;

        Number num0 = delegate.convert(obj0);
        Number num1 = delegate.convert(obj1);

        return delegate.multiply(num0, num1);
    }

    public final static boolean isNumber(final Object obj) {
        return (obj != null && NumberUtils.isNumber(obj.getClass()));
    }

    protected ExpressionArithmetic() {
        super();
    }

    //@off
    protected abstract Number convert(final Number num);
    protected abstract Number convert(final String str);

    protected abstract Number mod(final Number num0, final Number num1);
    protected abstract Number add(final Number num0, final Number num1);
    protected abstract Number divide(final Number num0, final Number num1);
    protected abstract Number multiply(final Number num0, final Number num1);
    protected abstract Number subtract(final Number num0, final Number num1);
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

        Class<?> objType = obj.getClass();
        if (Character.class == objType || char.class == objType) {
            return convert(Short.valueOf((short) ((Character) obj).charValue()));
        }

        throw new IllegalArgumentException(MessageFactory.get("el.convert", obj, objType));
    }

}
