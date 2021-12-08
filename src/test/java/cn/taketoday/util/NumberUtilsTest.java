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

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

import cn.taketoday.core.conversion.ConversionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Today
 * 2018年7月6日 下午1:36:29
 */
public class NumberUtilsTest {

  @Test
  public void test_IsNumber() throws ConversionException {
    assert NumberUtils.isNumber(int.class);
    assert NumberUtils.isNumber(byte.class);
    assert NumberUtils.isNumber(short.class);
    assert NumberUtils.isNumber(Short.class);
    assert NumberUtils.isNumber(Integer.class);
  }

  @Test
  void parseNumber() {
    String aByte = "" + Byte.MAX_VALUE;
    String aShort = "" + Short.MAX_VALUE;
    String anInteger = "" + Integer.MAX_VALUE;
    String aLong = "" + Long.MAX_VALUE;
    String aFloat = "" + Float.MAX_VALUE;
    String aDouble = "" + Double.MAX_VALUE;

    assertThat(NumberUtils.parseNumber(aByte, Byte.class)).as("Byte did not parse").isEqualTo(Byte.valueOf(Byte.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aShort, Short.class)).as("Short did not parse").isEqualTo(Short.valueOf(Short.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(anInteger, Integer.class)).as("Integer did not parse").isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aLong, Long.class)).as("Long did not parse").isEqualTo(Long.valueOf(Long.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aFloat, Float.class)).as("Float did not parse").isEqualTo(Float.valueOf(Float.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aDouble, Double.class)).as("Double did not parse").isEqualTo(Double.valueOf(Double.MAX_VALUE));
  }

  @Test
  void parseNumberUsingNumberFormat() {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    String aByte = "" + Byte.MAX_VALUE;
    String aShort = "" + Short.MAX_VALUE;
    String anInteger = "" + Integer.MAX_VALUE;
    String aLong = "" + Long.MAX_VALUE;
    String aFloat = "" + Float.MAX_VALUE;
    String aDouble = "" + Double.MAX_VALUE;

    assertThat(NumberUtils.parseNumber(aByte, Byte.class, nf)).as("Byte did not parse").isEqualTo(Byte.valueOf(Byte.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aShort, Short.class, nf)).as("Short did not parse").isEqualTo(Short.valueOf(Short.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(anInteger, Integer.class, nf)).as("Integer did not parse").isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aLong, Long.class, nf)).as("Long did not parse").isEqualTo(Long.valueOf(Long.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aFloat, Float.class, nf)).as("Float did not parse").isEqualTo(Float.valueOf(Float.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aDouble, Double.class, nf)).as("Double did not parse").isEqualTo(Double.valueOf(Double.MAX_VALUE));
  }

  @Test
  void parseNumberRequiringTrim() {
    String aByte = " " + Byte.MAX_VALUE + " ";
    String aShort = " " + Short.MAX_VALUE + " ";
    String anInteger = " " + Integer.MAX_VALUE + " ";
    String aLong = " " + Long.MAX_VALUE + " ";
    String aFloat = " " + Float.MAX_VALUE + " ";
    String aDouble = " " + Double.MAX_VALUE + " ";

    assertThat(NumberUtils.parseNumber(aByte, Byte.class)).as("Byte did not parse").isEqualTo(Byte.valueOf(Byte.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aShort, Short.class)).as("Short did not parse").isEqualTo(Short.valueOf(Short.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(anInteger, Integer.class)).as("Integer did not parse").isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aLong, Long.class)).as("Long did not parse").isEqualTo(Long.valueOf(Long.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aFloat, Float.class)).as("Float did not parse").isEqualTo(Float.valueOf(Float.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aDouble, Double.class)).as("Double did not parse").isEqualTo(Double.valueOf(Double.MAX_VALUE));
  }

  @Test
  void parseNumberRequiringTrimUsingNumberFormat() {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    String aByte = " " + Byte.MAX_VALUE + " ";
    String aShort = " " + Short.MAX_VALUE + " ";
    String anInteger = " " + Integer.MAX_VALUE + " ";
    String aLong = " " + Long.MAX_VALUE + " ";
    String aFloat = " " + Float.MAX_VALUE + " ";
    String aDouble = " " + Double.MAX_VALUE + " ";

    assertThat(NumberUtils.parseNumber(aByte, Byte.class, nf)).as("Byte did not parse").isEqualTo(Byte.valueOf(Byte.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aShort, Short.class, nf)).as("Short did not parse").isEqualTo(Short.valueOf(Short.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(anInteger, Integer.class, nf)).as("Integer did not parse").isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aLong, Long.class, nf)).as("Long did not parse").isEqualTo(Long.valueOf(Long.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aFloat, Float.class, nf)).as("Float did not parse").isEqualTo(Float.valueOf(Float.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aDouble, Double.class, nf)).as("Double did not parse").isEqualTo(Double.valueOf(Double.MAX_VALUE));
  }

  @Test
  void parseNumberAsHex() {
    String aByte = "0x" + Integer.toHexString(Byte.valueOf(Byte.MAX_VALUE).intValue());
    String aShort = "0x" + Integer.toHexString(Short.valueOf(Short.MAX_VALUE).intValue());
    String anInteger = "0x" + Integer.toHexString(Integer.MAX_VALUE);
    String aLong = "0x" + Long.toHexString(Long.MAX_VALUE);
    String aReallyBigInt = "FEBD4E677898DFEBFFEE44";

    assertByteEquals(aByte);
    assertShortEquals(aShort);
    assertIntegerEquals(anInteger);
    assertLongEquals(aLong);
    assertThat(NumberUtils.parseNumber("0x" + aReallyBigInt, BigInteger.class)).as("BigInteger did not parse").isEqualTo(new BigInteger(aReallyBigInt, 16));
  }

  @Test
  void parseNumberAsNegativeHex() {
    String aByte = "-0x80";
    String aShort = "-0x8000";
    String anInteger = "-0x80000000";
    String aLong = "-0x8000000000000000";
    String aReallyBigInt = "FEBD4E677898DFEBFFEE44";

    assertNegativeByteEquals(aByte);
    assertNegativeShortEquals(aShort);
    assertNegativeIntegerEquals(anInteger);
    assertNegativeLongEquals(aLong);
    assertThat(NumberUtils.parseNumber("-0x" + aReallyBigInt, BigInteger.class)).as("BigInteger did not parse").isEqualTo(new BigInteger(aReallyBigInt, 16).negate());
  }

  @Test
  void convertDoubleToBigInteger() {
    Double decimal = Double.valueOf(3.14d);
    assertThat(NumberUtils.convertNumberToTargetClass(decimal, BigInteger.class)).isEqualTo(new BigInteger("3"));
  }

  @Test
  void convertBigDecimalToBigInteger() {
    String number = "987459837583750387355346";
    BigDecimal decimal = new BigDecimal(number);
    assertThat(NumberUtils.convertNumberToTargetClass(decimal, BigInteger.class)).isEqualTo(new BigInteger(number));
  }

  @Test
  void convertNonExactBigDecimalToBigInteger() {
    BigDecimal decimal = new BigDecimal("987459837583750387355346.14");
    assertThat(NumberUtils.convertNumberToTargetClass(decimal, BigInteger.class)).isEqualTo(new BigInteger("987459837583750387355346"));
  }

  @Test
  void parseBigDecimalNumber1() {
    String bigDecimalAsString = "0.10";
    Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class);
    assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
  }

  @Test
  void parseBigDecimalNumber2() {
    String bigDecimalAsString = "0.001";
    Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class);
    assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
  }

  @Test
  void parseBigDecimalNumber3() {
    String bigDecimalAsString = "3.14159265358979323846";
    Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class);
    assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
  }

  @Test
  void parseLocalizedBigDecimalNumber1() {
    String bigDecimalAsString = "0.10";
    NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
    Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class, numberFormat);
    assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
  }

  @Test
  void parseLocalizedBigDecimalNumber2() {
    String bigDecimalAsString = "0.001";
    NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
    Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class, numberFormat);
    assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
  }

  @Test
  void parseLocalizedBigDecimalNumber3() {
    String bigDecimalAsString = "3.14159265358979323846";
    NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
    Number bigDecimal = NumberUtils.parseNumber(bigDecimalAsString, BigDecimal.class, numberFormat);
    assertThat(bigDecimal).isEqualTo(new BigDecimal(bigDecimalAsString));
  }

  @Test
  void parseOverflow() {
    String aLong = "" + Long.MAX_VALUE;
    String aDouble = "" + Double.MAX_VALUE;

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Byte.class));

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Short.class));

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Integer.class));

    assertThat(NumberUtils.parseNumber(aLong, Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aDouble, Double.class)).isEqualTo(Double.valueOf(Double.MAX_VALUE));
  }

  @Test
  void parseNegativeOverflow() {
    String aLong = "" + Long.MIN_VALUE;
    String aDouble = "" + Double.MIN_VALUE;

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Byte.class));

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Short.class));

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Integer.class));

    assertThat(NumberUtils.parseNumber(aLong, Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
    assertThat(NumberUtils.parseNumber(aDouble, Double.class)).isEqualTo(Double.valueOf(Double.MIN_VALUE));
  }

  @Test
  void parseOverflowUsingNumberFormat() {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    String aLong = "" + Long.MAX_VALUE;
    String aDouble = "" + Double.MAX_VALUE;

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Byte.class, nf));

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Short.class, nf));

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Integer.class, nf));

    assertThat(NumberUtils.parseNumber(aLong, Long.class, nf)).isEqualTo(Long.valueOf(Long.MAX_VALUE));
    assertThat(NumberUtils.parseNumber(aDouble, Double.class, nf)).isEqualTo(Double.valueOf(Double.MAX_VALUE));
  }

  @Test
  void parseNegativeOverflowUsingNumberFormat() {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    String aLong = "" + Long.MIN_VALUE;
    String aDouble = "" + Double.MIN_VALUE;

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Byte.class, nf));

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Short.class, nf));

    assertThatIllegalArgumentException().isThrownBy(() ->
            NumberUtils.parseNumber(aLong, Integer.class, nf));

    assertThat(NumberUtils.parseNumber(aLong, Long.class, nf)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
    assertThat(NumberUtils.parseNumber(aDouble, Double.class, nf)).isEqualTo(Double.valueOf(Double.MIN_VALUE));
  }

  @Test
  void convertToInteger() {
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(-1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(0), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MAX_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MAX_VALUE + 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MIN_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Integer.MIN_VALUE - 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));

    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(-1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(0), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(Integer.MAX_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(Integer.MAX_VALUE + 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(Integer.MIN_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(Integer.MIN_VALUE - 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));

    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(-1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(0), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MAX_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MAX_VALUE + 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MIN_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Integer.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MIN_VALUE - 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.MAX_VALUE));

    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) -1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) 0), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf(Short.MAX_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Short.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) (Short.MAX_VALUE + 1)), Integer.class)).isEqualTo(Integer.valueOf(Short.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf(Short.MIN_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Short.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) (Short.MIN_VALUE - 1)), Integer.class)).isEqualTo(Integer.valueOf(Short.MAX_VALUE));

    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) -1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) 0), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) 1), Integer.class)).isEqualTo(Integer.valueOf(Integer.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf(Byte.MAX_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Byte.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) (Byte.MAX_VALUE + 1)), Integer.class)).isEqualTo(Integer.valueOf(Byte.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf(Byte.MIN_VALUE), Integer.class)).isEqualTo(Integer.valueOf(Byte.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) (Byte.MIN_VALUE - 1)), Integer.class)).isEqualTo(Integer.valueOf(Byte.MAX_VALUE));

    assertToNumberOverflow(Long.valueOf(Long.MAX_VALUE + 1), Integer.class);
    assertToNumberOverflow(Long.valueOf(Long.MIN_VALUE - 1), Integer.class);
    assertToNumberOverflow(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE), Integer.class);
    assertToNumberOverflow(BigInteger.valueOf(Integer.MIN_VALUE).subtract(BigInteger.ONE), Integer.class);
    assertToNumberOverflow(new BigDecimal("18446744073709551611"), Integer.class);
  }

  @Test
  void convertToLong() {
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(-1), Long.class)).isEqualTo(Long.valueOf(Long.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(0), Long.class)).isEqualTo(Long.valueOf(Long.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(1), Long.class)).isEqualTo(Long.valueOf(Long.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MAX_VALUE), Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MAX_VALUE + 1), Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MIN_VALUE), Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(BigInteger.valueOf(Long.MIN_VALUE - 1), Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));

    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(-1), Long.class)).isEqualTo(Long.valueOf(Long.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(0), Long.class)).isEqualTo(Long.valueOf(Long.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(1), Long.class)).isEqualTo(Long.valueOf(Long.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(Long.MAX_VALUE), Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(Long.MAX_VALUE + 1), Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(Long.MIN_VALUE), Long.class)).isEqualTo(Long.valueOf(Long.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Long.valueOf(Long.MIN_VALUE - 1), Long.class)).isEqualTo(Long.valueOf(Long.MAX_VALUE));

    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(-1), Long.class)).isEqualTo(Long.valueOf(Integer.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(0), Long.class)).isEqualTo(Long.valueOf(Integer.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(1), Long.class)).isEqualTo(Long.valueOf(Integer.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MAX_VALUE), Long.class)).isEqualTo(Long.valueOf(Integer.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MAX_VALUE + 1), Long.class)).isEqualTo(Long.valueOf(Integer.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MIN_VALUE), Long.class)).isEqualTo(Long.valueOf(Integer.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Integer.valueOf(Integer.MIN_VALUE - 1), Long.class)).isEqualTo(Long.valueOf(Integer.MAX_VALUE));

    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) -1), Long.class)).isEqualTo(Long.valueOf(Integer.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) 0), Long.class)).isEqualTo(Long.valueOf(Integer.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) 1), Long.class)).isEqualTo(Long.valueOf(Integer.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf(Short.MAX_VALUE), Long.class)).isEqualTo(Long.valueOf(Short.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) (Short.MAX_VALUE + 1)), Long.class)).isEqualTo(Long.valueOf(Short.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf(Short.MIN_VALUE), Long.class)).isEqualTo(Long.valueOf(Short.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Short.valueOf((short) (Short.MIN_VALUE - 1)), Long.class)).isEqualTo(Long.valueOf(Short.MAX_VALUE));

    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) -1), Long.class)).isEqualTo(Long.valueOf(Integer.valueOf(-1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) 0), Long.class)).isEqualTo(Long.valueOf(Integer.valueOf(0)));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) 1), Long.class)).isEqualTo(Long.valueOf(Integer.valueOf(1)));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf(Byte.MAX_VALUE), Long.class)).isEqualTo(Long.valueOf(Byte.MAX_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) (Byte.MAX_VALUE + 1)), Long.class)).isEqualTo(Long.valueOf(Byte.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf(Byte.MIN_VALUE), Long.class)).isEqualTo(Long.valueOf(Byte.MIN_VALUE));
    assertThat(NumberUtils.convertNumberToTargetClass(Byte.valueOf((byte) (Byte.MIN_VALUE - 1)), Long.class)).isEqualTo(Long.valueOf(Byte.MAX_VALUE));

    assertToNumberOverflow(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), Long.class);
    assertToNumberOverflow(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), Long.class);
    assertToNumberOverflow(new BigDecimal("18446744073709551611"), Long.class);
  }

  private void assertLongEquals(String aLong) {
    assertThat(NumberUtils.parseNumber(aLong, Long.class).longValue()).as("Long did not parse").isEqualTo(Long.MAX_VALUE);
  }

  private void assertIntegerEquals(String anInteger) {
    assertThat(NumberUtils.parseNumber(anInteger, Integer.class).intValue()).as("Integer did not parse").isEqualTo(Integer.MAX_VALUE);
  }

  private void assertShortEquals(String aShort) {
    assertThat(NumberUtils.parseNumber(aShort, Short.class).shortValue()).as("Short did not parse").isEqualTo(Short.MAX_VALUE);
  }

  private void assertByteEquals(String aByte) {
    assertThat(NumberUtils.parseNumber(aByte, Byte.class).byteValue()).as("Byte did not parse").isEqualTo(Byte.MAX_VALUE);
  }

  private void assertNegativeLongEquals(String aLong) {
    assertThat(NumberUtils.parseNumber(aLong, Long.class).longValue()).as("Long did not parse").isEqualTo(Long.MIN_VALUE);
  }

  private void assertNegativeIntegerEquals(String anInteger) {
    assertThat(NumberUtils.parseNumber(anInteger, Integer.class).intValue()).as("Integer did not parse").isEqualTo(Integer.MIN_VALUE);
  }

  private void assertNegativeShortEquals(String aShort) {
    assertThat(NumberUtils.parseNumber(aShort, Short.class).shortValue()).as("Short did not parse").isEqualTo(Short.MIN_VALUE);
  }

  private void assertNegativeByteEquals(String aByte) {
    assertThat(NumberUtils.parseNumber(aByte, Byte.class).byteValue()).as("Byte did not parse").isEqualTo(Byte.MIN_VALUE);
  }

  private void assertToNumberOverflow(Number number, Class<? extends Number> targetClass) {
    String msg = "overflow: from=" + number + ", toClass=" + targetClass;
    assertThatIllegalArgumentException().as(msg).isThrownBy(() ->
                    NumberUtils.convertNumberToTargetClass(number, targetClass))
            .withMessageEndingWith("overflow");
  }

}
