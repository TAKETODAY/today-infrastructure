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

import java.util.ArrayList;
import java.util.Collections;

import cn.taketoday.expression.spel.ast.InlineList;
import cn.taketoday.expression.spel.standard.SpelExpression;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test usage of inline lists.
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0.4
 */
public class ListTests extends AbstractExpressionTests {

  // if the list is full of literals then it will be of the type unmodifiableClass
  // rather than ArrayList
  Class<?> unmodifiableClass = Collections.unmodifiableList(new ArrayList<>()).getClass();

  @Test
  public void testInlineListCreation01() {
    evaluate("{1, 2, 3, 4, 5}", "[1, 2, 3, 4, 5]", unmodifiableClass);
  }

  @Test
  public void testInlineListCreation02() {
    evaluate("{'abc', 'xyz'}", "[abc, xyz]", unmodifiableClass);
  }

  @Test
  public void testInlineListCreation03() {
    evaluate("{}", "[]", unmodifiableClass);
  }

  @Test
  public void testInlineListCreation04() {
    evaluate("{'abc'=='xyz'}", "[false]", ArrayList.class);
  }

  @Test
  public void testInlineListAndNesting() {
    evaluate("{{1,2,3},{4,5,6}}", "[[1, 2, 3], [4, 5, 6]]", unmodifiableClass);
    evaluate("{{1,'2',3},{4,{'a','b'},5,6}}", "[[1, 2, 3], [4, [a, b], 5, 6]]", unmodifiableClass);
  }

  @Test
  public void testInlineListError() {
    parseAndCheckError("{'abc'", SpelMessage.OOD);
  }

  @Test
  public void testRelOperatorsIs02() {
    evaluate("{1, 2, 3, 4, 5} instanceof T(java.util.List)", "true", Boolean.class);
  }

  @Test
  public void testInlineListCreation05() {
    evaluate("3 between {1,5}", "true", Boolean.class);
  }

  @Test
  public void testInlineListCreation06() {
    evaluate("8 between {1,5}", "false", Boolean.class);
  }

  @Test
  public void testInlineListAndProjectionSelection() {
    evaluate("{1,2,3,4,5,6}.![#this>3]", "[false, false, false, true, true, true]", ArrayList.class);
    evaluate("{1,2,3,4,5,6}.?[#this>3]", "[4, 5, 6]", ArrayList.class);
    evaluate("{1,2,3,4,5,6,7,8,9,10}.?[#isEven(#this) == 'y']", "[2, 4, 6, 8, 10]", ArrayList.class);
  }

  @Test
  public void testSetConstruction01() {
    evaluate("new java.util.HashSet().addAll({'a','b','c'})", "true", Boolean.class);
  }

  @Test
  public void testRelOperatorsBetween01() {
    evaluate("32 between {32, 42}", "true", Boolean.class);
  }

  @Test
  public void testRelOperatorsBetween02() {
    evaluate("'efg' between {'abc', 'xyz'}", "true", Boolean.class);
  }

  @Test
  public void testRelOperatorsBetween03() {
    evaluate("42 between {32, 42}", "true", Boolean.class);
  }

  @Test
  public void testRelOperatorsBetween04() {
    evaluate("new java.math.BigDecimal('1') between {new java.math.BigDecimal('1'),new java.math.BigDecimal('5')}",
            "true", Boolean.class);
    evaluate("new java.math.BigDecimal('3') between {new java.math.BigDecimal('1'),new java.math.BigDecimal('5')}",
            "true", Boolean.class);
    evaluate("new java.math.BigDecimal('5') between {new java.math.BigDecimal('1'),new java.math.BigDecimal('5')}",
            "true", Boolean.class);
    evaluate("new java.math.BigDecimal('8') between {new java.math.BigDecimal('1'),new java.math.BigDecimal('5')}",
            "false", Boolean.class);
  }

  @Test
  public void testRelOperatorsBetweenErrors02() {
    evaluateAndCheckError("'abc' between {5,7}", SpelMessage.NOT_COMPARABLE, 6);
  }

  @Test
  public void testConstantRepresentation1() {
    checkConstantList("{1,2,3,4,5}", true);
    checkConstantList("{'abc'}", true);
    checkConstantList("{}", true);
    checkConstantList("{#a,2,3}", false);
    checkConstantList("{1,2,Integer.valueOf(4)}", false);
    checkConstantList("{1,2,{#a}}", false);
  }

  private void checkConstantList(String expressionText, boolean expectedToBeConstant) {
    SpelExpressionParser parser = new SpelExpressionParser();
    SpelExpression expression = (SpelExpression) parser.parseExpression(expressionText);
    SpelNode node = expression.getAST();
    boolean condition = node instanceof InlineList;
    assertThat(condition).isTrue();
    InlineList inlineList = (InlineList) node;
    if (expectedToBeConstant) {
      assertThat(inlineList.isConstant()).isTrue();
    }
    else {
      assertThat(inlineList.isConstant()).isFalse();
    }
  }

  @Test
  public void testInlineListWriting() {
    // list should be unmodifiable
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            evaluate("{1, 2, 3, 4, 5}[0]=6", "[1, 2, 3, 4, 5]", unmodifiableClass));
  }
}
