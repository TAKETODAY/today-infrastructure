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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import cn.taketoday.core.conversion.ConversionException;

/**
 * @author Today
 * 2018年7月6日 下午1:36:29
 */
public class NumberUtilsTest {

  private long start;

  @Before
  public void start() {
    start = System.currentTimeMillis();
  }

  @After
  public void end() {
    System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
  }

  @Test
  public void test_IsNumber() throws ConversionException {
    assert NumberUtils.isNumber(int.class);
    assert NumberUtils.isNumber(byte.class);
    assert NumberUtils.isNumber(short.class);
    assert NumberUtils.isNumber(Short.class);
    assert NumberUtils.isNumber(Integer.class);
  }

  @Test
  public void test_ParseDigit() throws ConversionException {

    assert NumberUtils.parseDigit("", int.class).equals(0);

    Object parseint = NumberUtils.parseDigit("12121", int.class);

    assert parseint.equals(12121);
    assert parseint.getClass() == Integer.class;

    Object parseInteger = NumberUtils.parseDigit("12121", Integer.class);
    assert parseInteger.equals(12121);
    assert parseInteger.getClass() == Integer.class;

    Object parselong = NumberUtils.parseDigit("12121", long.class);
    assert parselong.equals(12121l);
    assert parselong.getClass() == Long.class;

    Object parseLong = NumberUtils.parseDigit("12121", Long.class);
    assert parseLong.equals(12121l);
    assert parseLong.getClass() == Long.class;

    Object parseshort = NumberUtils.parseDigit("12345", short.class);
    assert parseshort.equals((short) 12345);
    assert parseshort.getClass() == Short.class;

    Object parseShort = NumberUtils.parseDigit("12345", Short.class);
    assert parseShort.equals((short) 12345);
    assert parseShort.getClass() == Short.class;

    Object parsebyte = NumberUtils.parseDigit("123", byte.class);
    assert parsebyte.equals((byte) 123);
    assert parsebyte.getClass() == Byte.class;

    Object parseByte = NumberUtils.parseDigit("123", Byte.class);
    assert parseByte.equals((byte) 123);
    assert parseByte.getClass() == Byte.class;

    Object parseBigInteger = NumberUtils.parseDigit("123", BigInteger.class);
    assert parseBigInteger.equals(new BigInteger("123"));
    assert parseBigInteger.getClass() == BigInteger.class;

    Object parsefloat = NumberUtils.parseDigit("123.45", float.class);
    assert parsefloat.equals(123.45f);
    assert parsefloat.getClass() == Float.class;

    Object parseFloat = NumberUtils.parseDigit("123.45", Float.class);
    assert parseFloat.equals(123.45f);
    assert parseFloat.getClass() == Float.class;

    Object parsedouble = NumberUtils.parseDigit("123.45", double.class);
    assert parsedouble.equals(123.45d);
    assert parsedouble.getClass() == Double.class;

    Object parseDouble = NumberUtils.parseDigit("123.45", Double.class);
    assert parseDouble.equals(123.45d);
    assert parseDouble.getClass() == Double.class;

    Object parseBigDecimal = NumberUtils.parseDigit("123.45", BigDecimal.class);
    assert parseBigDecimal.equals(new BigDecimal("123.45"));
    assert parseBigDecimal.getClass() == BigDecimal.class;

    Object parseNumber = NumberUtils.parseDigit("123.45", Number.class);
    assert parseNumber.equals(new BigDecimal("123.45"));
    assert parseNumber.getClass() == BigDecimal.class;

    try {
      NumberUtils.parseDigit("123.45", getClass());
      assert false;
    }
    catch (Exception e) {
      assert true;
    }

  }

}
