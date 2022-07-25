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
import java.util.HashMap;

import cn.taketoday.expression.spel.standard.SpelExpression;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These are tests for language features that are not yet considered 'live'. Either missing implementation or
 * documentation.
 *
 * Where implementation is missing the tests are commented out.
 *
 * @author Andy Clement
 */
public class InProgressTests extends AbstractExpressionTests {

  @Test
  public void testRelOperatorsBetween01() {
    evaluate("1 between listOneFive", "true", Boolean.class);
    // no inline list building at the moment
    // evaluate("1 between {1, 5}", "true", Boolean.class);
  }

  @Test
  public void testRelOperatorsBetweenErrors01() {
    evaluateAndCheckError("1 between T(String)", SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST, 10);
  }

  @Test
  public void testRelOperatorsBetweenErrors03() {
    evaluateAndCheckError("1 between listOfNumbersUpToTen",
            SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST, 10);
  }

  // PROJECTION
  @Test
  public void testProjection01() {
    evaluate("listOfNumbersUpToTen.![#this<5?'y':'n']", "[y, y, y, y, n, n, n, n, n, n]", ArrayList.class);
    // inline list creation not supported at the moment
    // evaluate("{1,2,3,4,5,6,7,8,9,10}.!{#isEven(#this)}", "[n, y, n, y, n, y, n, y, n, y]", ArrayList.class);
  }

  @Test
  public void testProjection02() {
    // inline map creation not supported at the moment
    // evaluate("#{'a':'y','b':'n','c':'y'}.![value=='y'?key:null].nonnull().sort()", "[a, c]", ArrayList.class);
    evaluate("mapOfNumbersUpToTen.![key>5?value:null]",
            "[null, null, null, null, null, six, seven, eight, nine, ten]", ArrayList.class);
  }

  @Test
  public void testProjection05() {
    evaluateAndCheckError("'abc'.![true]", SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE);
    evaluateAndCheckError("null.![true]", SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE);
    evaluate("null?.![true]", null, null);
  }

  @Test
  public void testProjection06() throws Exception {
    SpelExpression expr = (SpelExpression) parser.parseExpression("'abc'.![true]");
    assertThat(expr.toStringAST()).isEqualTo("'abc'.![true]");
  }

  // SELECTION

  @Test
  public void testSelection02() {
    evaluate("testMap.keySet().?[#this matches '.*o.*']", "[monday]", ArrayList.class);
    evaluate("testMap.keySet().?[#this matches '.*r.*'].contains('saturday')", "true", Boolean.class);
    evaluate("testMap.keySet().?[#this matches '.*r.*'].size()", "3", Integer.class);
  }

  @Test
  public void testSelectionError_NonBooleanSelectionCriteria() {
    evaluateAndCheckError("listOfNumbersUpToTen.?['nonboolean']",
            SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
  }

  @Test
  public void testSelection03() {
    evaluate("mapOfNumbersUpToTen.?[key>5].size()", "5", Integer.class);
  }

  @Test
  public void testSelection04() {
    evaluateAndCheckError("mapOfNumbersUpToTen.?['hello'].size()",
            SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
  }

  @Test
  public void testSelection05() {
    evaluate("mapOfNumbersUpToTen.?[key>11].size()", "0", Integer.class);
    evaluate("mapOfNumbersUpToTen.^[key>11]", null, null);
    evaluate("mapOfNumbersUpToTen.$[key>11]", null, null);
    evaluate("null?.$[key>11]", null, null);
    evaluateAndCheckError("null.?[key>11]", SpelMessage.INVALID_TYPE_FOR_SELECTION);
    evaluateAndCheckError("'abc'.?[key>11]", SpelMessage.INVALID_TYPE_FOR_SELECTION);
  }

  @Test
  public void testSelectionFirst01() {
    evaluate("listOfNumbersUpToTen.^[#isEven(#this) == 'y']", "2", Integer.class);
  }

  @Test
  public void testSelectionFirst02() {
    evaluate("mapOfNumbersUpToTen.^[key>5].size()", "1", Integer.class);
  }

  @Test
  public void testSelectionLast01() {
    evaluate("listOfNumbersUpToTen.$[#isEven(#this) == 'y']", "10", Integer.class);
  }

  @Test
  public void testSelectionLast02() {
    evaluate("mapOfNumbersUpToTen.$[key>5]", "{10=ten}", HashMap.class);
    evaluate("mapOfNumbersUpToTen.$[key>5].size()", "1", Integer.class);
  }

  @Test
  public void testSelectionAST() throws Exception {
    SpelExpression expr = (SpelExpression) parser.parseExpression("'abc'.^[true]");
    assertThat(expr.toStringAST()).isEqualTo("'abc'.^[true]");
    expr = (SpelExpression) parser.parseExpression("'abc'.?[true]");
    assertThat(expr.toStringAST()).isEqualTo("'abc'.?[true]");
    expr = (SpelExpression) parser.parseExpression("'abc'.$[true]");
    assertThat(expr.toStringAST()).isEqualTo("'abc'.$[true]");
  }

  // Constructor invocation
  @Test
  public void testSetConstruction01() {
    evaluate("new java.util.HashSet().addAll({'a','b','c'})", "true", Boolean.class);
  }

}
