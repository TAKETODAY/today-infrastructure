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

/**
 * Tests the messages and exceptions that come out for badly formed expressions
 *
 * @author Andy Clement
 */
public class ParserErrorMessagesTests extends AbstractExpressionTests {

  @Test
  public void testBrokenExpression01() {
    // will not fit into an int, needs L suffix
    parseAndCheckError("0xCAFEBABE", SpelMessage.NOT_AN_INTEGER);
    evaluate("0xCAFEBABEL", 0xCAFEBABEL, Long.class);
    parseAndCheckError("0xCAFEBABECAFEBABEL", SpelMessage.NOT_A_LONG);
  }

  @Test
  public void testBrokenExpression02() {
    // rogue 'G' on the end
    parseAndCheckError("0xB0BG", SpelMessage.MORE_INPUT, 5, "G");
  }

  @Test
  public void testBrokenExpression04() {
    // missing right operand
    parseAndCheckError("true or ", SpelMessage.RIGHT_OPERAND_PROBLEM, 5);
  }

  @Test
  public void testBrokenExpression05() {
    // missing right operand
    parseAndCheckError("1 + ", SpelMessage.RIGHT_OPERAND_PROBLEM, 2);
  }

  @Test
  public void testBrokenExpression07() {
    // T() can only take an identifier (possibly qualified), not a literal
    // message ought to say identifier rather than ID
    parseAndCheckError("null instanceof T('a')", SpelMessage.NOT_EXPECTED_TOKEN, 18,
            "qualified ID", "literal_string");
  }

}
