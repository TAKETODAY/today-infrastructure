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

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.support.GenericConversionService;
import cn.taketoday.expression.spel.support.StandardTypeConverter;

/**
 * Tests the evaluation of real boolean expressions, these use AND, OR, NOT, TRUE, FALSE
 *
 * @author Andy Clement
 * @author Oliver Becker
 */
public class BooleanExpressionTests extends AbstractExpressionTests {

  @Test
  public void testBooleanTrue() {
    evaluate("true", Boolean.TRUE, Boolean.class);
  }

  @Test
  public void testBooleanFalse() {
    evaluate("false", Boolean.FALSE, Boolean.class);
  }

  @Test
  public void testOr() {
    evaluate("false or false", Boolean.FALSE, Boolean.class);
    evaluate("false or true", Boolean.TRUE, Boolean.class);
    evaluate("true or false", Boolean.TRUE, Boolean.class);
    evaluate("true or true", Boolean.TRUE, Boolean.class);
  }

  @Test
  public void testAnd() {
    evaluate("false and false", Boolean.FALSE, Boolean.class);
    evaluate("false and true", Boolean.FALSE, Boolean.class);
    evaluate("true and false", Boolean.FALSE, Boolean.class);
    evaluate("true and true", Boolean.TRUE, Boolean.class);
  }

  @Test
  public void testNot() {
    evaluate("!false", Boolean.TRUE, Boolean.class);
    evaluate("!true", Boolean.FALSE, Boolean.class);

    evaluate("not false", Boolean.TRUE, Boolean.class);
    evaluate("NoT true", Boolean.FALSE, Boolean.class);
  }

  @Test
  public void testCombinations01() {
    evaluate("false and false or true", Boolean.TRUE, Boolean.class);
    evaluate("true and false or true", Boolean.TRUE, Boolean.class);
    evaluate("true and false or false", Boolean.FALSE, Boolean.class);
  }

  @Test
  public void testWritability() {
    evaluate("true and true", Boolean.TRUE, Boolean.class, false);
    evaluate("true or true", Boolean.TRUE, Boolean.class, false);
    evaluate("!false", Boolean.TRUE, Boolean.class, false);
  }

  @Test
  public void testBooleanErrors01() {
    evaluateAndCheckError("1.0 or false", SpelMessage.TYPE_CONVERSION_ERROR, 0);
    evaluateAndCheckError("false or 39.4", SpelMessage.TYPE_CONVERSION_ERROR, 9);
    evaluateAndCheckError("true and 'hello'", SpelMessage.TYPE_CONVERSION_ERROR, 9);
    evaluateAndCheckError(" 'hello' and 'goodbye'", SpelMessage.TYPE_CONVERSION_ERROR, 1);
    evaluateAndCheckError("!35.2", SpelMessage.TYPE_CONVERSION_ERROR, 1);
    evaluateAndCheckError("! 'foob'", SpelMessage.TYPE_CONVERSION_ERROR, 2);
  }

  @Test
  public void testConvertAndHandleNull() { // SPR-9445
    // without null conversion
    evaluateAndCheckError("null or true", SpelMessage.TYPE_CONVERSION_ERROR, 0, "null", "boolean");
    evaluateAndCheckError("null and true", SpelMessage.TYPE_CONVERSION_ERROR, 0, "null", "boolean");
    evaluateAndCheckError("!null", SpelMessage.TYPE_CONVERSION_ERROR, 1, "null", "boolean");
    evaluateAndCheckError("null ? 'foo' : 'bar'", SpelMessage.TYPE_CONVERSION_ERROR, 0, "null", "boolean");

    // with null conversion (null -> false)
    GenericConversionService conversionService = new GenericConversionService() {
      @Override
      protected Object convertNullSource(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return targetType.getType() == Boolean.class ? false : null;
      }
    };
    context.setTypeConverter(new StandardTypeConverter(conversionService));

    evaluate("null or true", Boolean.TRUE, Boolean.class, false);
    evaluate("null and true", Boolean.FALSE, Boolean.class, false);
    evaluate("!null", Boolean.TRUE, Boolean.class, false);
    evaluate("null ? 'foo' : 'bar'", "bar", String.class, false);
  }

}
