/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static cn.taketoday.expression.spel.SpelMessage.FUNCTION_MUST_BE_STATIC;
import static cn.taketoday.expression.spel.SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the evaluation of expressions that access variables and functions (lambda/java).
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
public class VariableAndFunctionTests extends AbstractExpressionTests {

  @Test
  void variableAccess() {
    evaluate("#answer", "42", Integer.class, SHOULD_BE_WRITABLE);
    evaluate("#answer / 2", 21, Integer.class, SHOULD_NOT_BE_WRITABLE);
  }

  @Test
  void variableAccessWithWellKnownVariables() {
    evaluate("#this.getName()", "Nikola Tesla", String.class);
    evaluate("#root.getName()", "Nikola Tesla", String.class);
  }

  @Test
  void functionInvocationWithIncorrectNumberOfArguments() {
    // Method: #reverseInt(int, int, int)
    evaluateAndCheckError("#reverseInt()", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "reverseInt", 0, 3);
    evaluateAndCheckError("#reverseInt(1,2)", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "reverseInt", 2, 3);
    evaluateAndCheckError("#reverseInt(1,2,3,4)", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "reverseInt", 4, 3);

    // MethodHandle: #message(template, args...)
    evaluateAndCheckError("#message()", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "message", 0, 2);
    evaluateAndCheckError("#message('%s')", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "message", 1, 2);
  }

  @Test
  void functionInvocationWithPrimitiveArguments() {
    evaluate("#reverseInt(1,2,3)", "int[3]{3,2,1}", int[].class);
    evaluate("#reverseInt('1',2,3)", "int[3]{3,2,1}", int[].class); // requires type conversion of '1' to 1
  }

  @Test
  void functionInvocationWithStringArgument() {
    evaluate("#reverseString('hello')", "olleh", String.class);
    evaluate("#reverseString(37)", "73", String.class); // requires type conversion of 37 to '37'
  }

  @Test
  void functionWithVarargs() {
    evaluate("#varargsFunction()", "[]", String.class);
    evaluate("#varargsFunction(new String[0])", "[]", String.class);
    evaluate("#varargsFunction('a')", "[a]", String.class);
    evaluate("#varargsFunction('a','b','c')", "[a, b, c]", String.class);
    // Conversion from int to String
    evaluate("#varargsFunction(25)", "[25]", String.class);
    evaluate("#varargsFunction('b',25)", "[b, 25]", String.class);
    // Strings that contain a comma
    evaluate("#varargsFunction('a,b')", "[a,b]", String.class);
    evaluate("#varargsFunction('a', 'x,y', 'd')", "[a, x,y, d]", String.class);
    // null values
    evaluate("#varargsFunction(null)", "[null]", String.class);
    evaluate("#varargsFunction('a',null,'b')", "[a, null, b]", String.class);

    evaluate("#varargsFunction2(9)", "9-[]", String.class);
    evaluate("#varargsFunction2(9, new String[0])", "9-[]", String.class);
    evaluate("#varargsFunction2(9,'a')", "9-[a]", String.class);
    evaluate("#varargsFunction2(9,'a','b','c')", "9-[a, b, c]", String.class);
    // Conversion from int to String
    evaluate("#varargsFunction2(9,25)", "9-[25]", String.class);
    evaluate("#varargsFunction2(9,'b',25)", "9-[b, 25]", String.class);
    // Strings that contain a comma:
    evaluate("#varargsFunction2(9, 'a,b')", "9-[a,b]", String.class);
    evaluate("#varargsFunction2(9, 'a', 'x,y', 'd')", "9-[a, x,y, d]", String.class);
    // null values
    evaluate("#varargsFunction2(9,null)", "9-[null]", String.class);
    evaluate("#varargsFunction2(9,'a',null,'b')", "9-[a, null, b]", String.class);
  }

  @Test
  void functionMethodMustBeStatic() throws Exception {
    SpelExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext ctx = new StandardEvaluationContext();
    ctx.setVariable("notStatic", this.getClass().getMethod("nonStatic"));
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> parser.parseRaw("#notStatic()").getValue(ctx))
            .satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(FUNCTION_MUST_BE_STATIC));
  }

  // this method is used by the test above
  public void nonStatic() {
  }

}
