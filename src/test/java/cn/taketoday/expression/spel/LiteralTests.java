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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import cn.taketoday.expression.spel.standard.SpelExpression;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the evaluation of basic literals: boolean, integer, hex integer, long, real, null, date
 *
 * @author Andy Clement
 */
public class LiteralTests extends AbstractExpressionTests {

  @Test
  public void testLiteralBoolean01() {
    evaluate("false", "false", Boolean.class);
  }

  @Test
  public void testLiteralBoolean02() {
    evaluate("true", "true", Boolean.class);
  }

  @Test
  public void testLiteralInteger01() {
    evaluate("1", "1", Integer.class);
  }

  @Test
  public void testLiteralInteger02() {
    evaluate("1415", "1415", Integer.class);
  }

  @Test
  public void testLiteralString01() {
    evaluate("'Hello World'", "Hello World", String.class);
  }

  @Test
  public void testLiteralString02() {
    evaluate("'joe bloggs'", "joe bloggs", String.class);
  }

  @Test
  public void testLiteralString03() {
    evaluate("'hello'", "hello", String.class);
  }

  @Test
  public void testLiteralString04() {
    evaluate("'Tony''s Pizza'", "Tony's Pizza", String.class);
    evaluate("'Tony\\r''s Pizza'", "Tony\\r's Pizza", String.class);
  }

  @Test
  public void testLiteralString05() {
    evaluate("\"Hello World\"", "Hello World", String.class);
  }

  @Test
  public void testLiteralString06() {
    evaluate("\"Hello ' World\"", "Hello ' World", String.class);
  }

  @Test
  public void testHexIntLiteral01() {
    evaluate("0x7FFFF", "524287", Integer.class);
    evaluate("0x7FFFFL", 524287L, Long.class);
    evaluate("0X7FFFF", "524287", Integer.class);
    evaluate("0X7FFFFl", 524287L, Long.class);
  }

  @Test
  public void testLongIntLiteral01() {
    evaluate("0xCAFEBABEL", 3405691582L, Long.class);
  }

  @Test
  public void testLongIntInteractions01() {
    evaluate("0x20 * 2L", 64L, Long.class);
    // ask for the result to be made into an Integer
    evaluateAndAskForReturnType("0x20 * 2L", 64, Integer.class);
    // ask for the result to be made into an Integer knowing that it will not fit
    evaluateAndCheckError("0x1220 * 0xffffffffL", Integer.class, SpelMessage.TYPE_CONVERSION_ERROR, 0);
  }

  @Test
  public void testSignedIntLiterals() {
    evaluate("-1", -1, Integer.class);
    evaluate("-0xa", -10, Integer.class);
    evaluate("-1L", -1L, Long.class);
    evaluate("-0x20l", -32L, Long.class);
  }

  @Test
  public void testLiteralReal01_CreatingDoubles() {
    evaluate("1.25", 1.25d, Double.class);
    evaluate("2.99", 2.99d, Double.class);
    evaluate("-3.141", -3.141d, Double.class);
    evaluate("1.25d", 1.25d, Double.class);
    evaluate("2.99d", 2.99d, Double.class);
    evaluate("-3.141d", -3.141d, Double.class);
    evaluate("1.25D", 1.25d, Double.class);
    evaluate("2.99D", 2.99d, Double.class);
    evaluate("-3.141D", -3.141d, Double.class);
  }

  @Test
  public void testLiteralReal02_CreatingFloats() {
    // For now, everything becomes a double...
    evaluate("1.25f", 1.25f, Float.class);
    evaluate("2.5f", 2.5f, Float.class);
    evaluate("-3.5f", -3.5f, Float.class);
    evaluate("1.25F", 1.25f, Float.class);
    evaluate("2.5F", 2.5f, Float.class);
    evaluate("-3.5F", -3.5f, Float.class);
  }

  @Test
  public void testLiteralReal03_UsingExponents() {
    evaluate("6.0221415E+23", "6.0221415E23", Double.class);
    evaluate("6.0221415e+23", "6.0221415E23", Double.class);
    evaluate("6.0221415E+23d", "6.0221415E23", Double.class);
    evaluate("6.0221415e+23D", "6.0221415E23", Double.class);
    evaluate("6E2f", 6E2f, Float.class);
  }

  @Test
  public void testLiteralReal04_BadExpressions() {
    parseAndCheckError("6.1e23e22", SpelMessage.MORE_INPUT, 6, "e22");
    parseAndCheckError("6.1f23e22", SpelMessage.MORE_INPUT, 4, "23e22");
  }

  @Test
  public void testLiteralNull01() {
    evaluate("null", null, null);
  }

  @Test
  public void testConversions() {
    // getting the expression type to be what we want - either:
    evaluate("new Integer(37).byteValue()", (byte) 37, Byte.class); // calling byteValue() on Integer.class
    evaluateAndAskForReturnType("new Integer(37)", (byte) 37, Byte.class); // relying on registered type converters
  }

  @Test
  public void testNotWritable() throws Exception {
    SpelExpression expr = (SpelExpression) parser.parseExpression("37");
    assertThat(expr.isWritable(new StandardEvaluationContext())).isFalse();
    expr = (SpelExpression) parser.parseExpression("37L");
    assertThat(expr.isWritable(new StandardEvaluationContext())).isFalse();
    expr = (SpelExpression) parser.parseExpression("true");
    assertThat(expr.isWritable(new StandardEvaluationContext())).isFalse();
  }
}
