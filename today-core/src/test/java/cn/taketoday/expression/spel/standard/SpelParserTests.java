/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.expression.spel.standard;

import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.SpelNode;
import cn.taketoday.expression.spel.SpelParseException;
import cn.taketoday.expression.spel.ast.OpAnd;
import cn.taketoday.expression.spel.ast.OpOr;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static cn.taketoday.expression.spel.SpelMessage.MISSING_CONSTRUCTOR_ARGS;
import static cn.taketoday.expression.spel.SpelMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING;
import static cn.taketoday.expression.spel.SpelMessage.NON_TERMINATING_QUOTED_STRING;
import static cn.taketoday.expression.spel.SpelMessage.NOT_AN_INTEGER;
import static cn.taketoday.expression.spel.SpelMessage.NOT_A_LONG;
import static cn.taketoday.expression.spel.SpelMessage.REAL_CANNOT_BE_LONG;
import static cn.taketoday.expression.spel.SpelMessage.RUN_OUT_OF_ARGUMENTS;
import static cn.taketoday.expression.spel.SpelMessage.UNEXPECTED_DATA_AFTER_DOT;
import static cn.taketoday.expression.spel.SpelMessage.UNEXPECTED_ESCAPE_CHAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Andy Clement
 * @author Juergen Hoeller
 */
class SpelParserTests {

  private final SpelExpressionParser parser = new SpelExpressionParser();

  @Test
  void nullExpressionIsRejected() {
    assertNullOrEmptyExpressionIsRejected(() -> parser.parseExpression(null));
    assertNullOrEmptyExpressionIsRejected(() -> parser.parseRaw(null));
  }

  @Test
  void emptyExpressionIsRejected() {
    assertNullOrEmptyExpressionIsRejected(() -> parser.parseExpression(""));
    assertNullOrEmptyExpressionIsRejected(() -> parser.parseRaw(""));
  }

  @Test
  void blankExpressionIsRejected() {
    assertNullOrEmptyExpressionIsRejected(() -> parser.parseExpression("     "));
    assertNullOrEmptyExpressionIsRejected(() -> parser.parseExpression("\t\n"));
    assertNullOrEmptyExpressionIsRejected(() -> parser.parseRaw("     "));
    assertNullOrEmptyExpressionIsRejected(() -> parser.parseRaw("\t\n"));
  }

  private static void assertNullOrEmptyExpressionIsRejected(ThrowableAssert.ThrowingCallable throwingCallable) {
    assertThatIllegalArgumentException()
            .isThrownBy(throwingCallable)
            .withMessage("'expressionString' must not be null or blank");
  }

  @Test
  void theMostBasic() {
    SpelExpression expr = parser.parseRaw("2");
    assertThat(expr).isNotNull();
    assertThat(expr.getAST()).isNotNull();
    assertThat(expr.getValue()).isEqualTo(2);
    assertThat(expr.getValueType()).isEqualTo(Integer.class);
    assertThat(expr.getAST().getValue(null)).isEqualTo(2);
  }

  @Test
  void valueType() {
    EvaluationContext ctx = new StandardEvaluationContext();
    Class<?> c = parser.parseRaw("2").getValueType();
    assertThat(c).isEqualTo(Integer.class);
    c = parser.parseRaw("12").getValueType(ctx);
    assertThat(c).isEqualTo(Integer.class);
    c = parser.parseRaw("null").getValueType();
    assertThat(c).isNull();
    c = parser.parseRaw("null").getValueType(ctx);
    assertThat(c).isNull();
    Object o = parser.parseRaw("null").getValue(ctx, Integer.class);
    assertThat(o).isNull();
  }

  @Test
  void whitespace() {
    SpelExpression expr = parser.parseRaw("2      +    3");
    assertThat(expr.getValue()).isEqualTo(5);
    expr = parser.parseRaw("2	+	3");
    assertThat(expr.getValue()).isEqualTo(5);
    expr = parser.parseRaw("2\n+\t3");
    assertThat(expr.getValue()).isEqualTo(5);
    expr = parser.parseRaw("2\r\n+\t3");
    assertThat(expr.getValue()).isEqualTo(5);
  }

  @Test
  void arithmeticPlus1() {
    SpelExpression expr = parser.parseRaw("2+2");
    assertThat(expr).isNotNull();
    assertThat(expr.getAST()).isNotNull();
    assertThat(expr.getValue()).isEqualTo(4);
  }

  @Test
  void arithmeticPlus2() {
    SpelExpression expr = parser.parseRaw("37+41");
    assertThat(expr.getValue()).isEqualTo(78);
  }

  @Test
  void arithmeticMultiply1() {
    SpelExpression expr = parser.parseRaw("2*3");
    assertThat(expr).isNotNull();
    assertThat(expr.getAST()).isNotNull();
    assertThat(expr.getValue()).isEqualTo(6);
  }

  @Test
  void arithmeticPrecedence1() {
    SpelExpression expr = parser.parseRaw("2*3+5");
    assertThat(expr.getValue()).isEqualTo(11);
  }

  @Test
  void parseExceptions() {
    assertParseException(() -> parser.parseRaw("new String"), MISSING_CONSTRUCTOR_ARGS, 10);
    assertParseException(() -> parser.parseRaw("new String(3,"), RUN_OUT_OF_ARGUMENTS, 10);
    assertParseException(() -> parser.parseRaw("new String(3"), RUN_OUT_OF_ARGUMENTS, 10);
    assertParseException(() -> parser.parseRaw("new String("), RUN_OUT_OF_ARGUMENTS, 10);
    assertParseException(() -> parser.parseRaw("\"abc"), NON_TERMINATING_DOUBLE_QUOTED_STRING, 0);
    assertParseException(() -> parser.parseRaw("'abc"), NON_TERMINATING_QUOTED_STRING, 0);
  }

  @Test
  void arithmeticPrecedence2() {
    SpelExpression expr = parser.parseRaw("2+3*5");
    assertThat(expr.getValue()).isEqualTo(17);
  }

  @Test
  void arithmeticPrecedence3() {
    SpelExpression expr = parser.parseRaw("3+10/2");
    assertThat(expr.getValue()).isEqualTo(8);
  }

  @Test
  void arithmeticPrecedence4() {
    SpelExpression expr = parser.parseRaw("10/2+3");
    assertThat(expr.getValue()).isEqualTo(8);
  }

  @Test
  void arithmeticPrecedence5() {
    SpelExpression expr = parser.parseRaw("(4+10)/2");
    assertThat(expr.getValue()).isEqualTo(7);
  }

  @Test
  void arithmeticPrecedence6() {
    SpelExpression expr = parser.parseRaw("(3+2)*2");
    assertThat(expr.getValue()).isEqualTo(10);
  }

  @Test
  void booleanOperators() {
    SpelExpression expr = parser.parseRaw("true");
    assertThat(expr.getValue(Boolean.class)).isTrue();
    expr = parser.parseRaw("false");
    assertThat(expr.getValue(Boolean.class)).isFalse();
    expr = parser.parseRaw("false and false");
    assertThat(expr.getValue(Boolean.class)).isFalse();
    expr = parser.parseRaw("true and (true or false)");
    assertThat(expr.getValue(Boolean.class)).isTrue();
    expr = parser.parseRaw("true and true or false");
    assertThat(expr.getValue(Boolean.class)).isTrue();
    expr = parser.parseRaw("!true");
    assertThat(expr.getValue(Boolean.class)).isFalse();
    expr = parser.parseRaw("!(false or true)");
    assertThat(expr.getValue(Boolean.class)).isFalse();
  }

  @Test
  void booleanOperators_symbolic_spr9614() {
    SpelExpression expr = parser.parseRaw("true");
    assertThat(expr.getValue(Boolean.class)).isTrue();
    expr = parser.parseRaw("false");
    assertThat(expr.getValue(Boolean.class)).isFalse();
    expr = parser.parseRaw("false && false");
    assertThat(expr.getValue(Boolean.class)).isFalse();
    expr = parser.parseRaw("true && (true || false)");
    assertThat(expr.getValue(Boolean.class)).isTrue();
    expr = parser.parseRaw("true && true || false");
    assertThat(expr.getValue(Boolean.class)).isTrue();
    expr = parser.parseRaw("!true");
    assertThat(expr.getValue(Boolean.class)).isFalse();
    expr = parser.parseRaw("!(false || true)");
    assertThat(expr.getValue(Boolean.class)).isFalse();
  }

  @Test
  void stringLiterals() {
    SpelExpression expr = parser.parseRaw("'howdy'");
    assertThat(expr.getValue()).isEqualTo("howdy");
    expr = parser.parseRaw("'hello '' world'");
    assertThat(expr.getValue()).isEqualTo("hello ' world");
  }

  @Test
  void stringLiterals2() {
    SpelExpression expr = parser.parseRaw("'howdy'.substring(0,2)");
    assertThat(expr.getValue()).isEqualTo("ho");
  }

  @Test
  void testStringLiterals_DoubleQuotes_spr9620() {
    SpelExpression expr = parser.parseRaw("\"double quote: \"\".\"");
    assertThat(expr.getValue()).isEqualTo("double quote: \".");
    expr = parser.parseRaw("\"hello \"\" world\"");
    assertThat(expr.getValue()).isEqualTo("hello \" world");
  }

  @Test
  void testStringLiterals_DoubleQuotes_spr9620_2() {
    assertParseExceptionThrownBy(() -> parser.parseRaw("\"double quote: \\\"\\\".\""))
            .satisfies(ex -> {
              assertThat(ex.getPosition()).isEqualTo(17);
              assertThat(ex.getMessageCode()).isEqualTo(UNEXPECTED_ESCAPE_CHAR);
            });
  }

  @Test
  void positionalInformation() {
    SpelExpression expr = parser.parseRaw("true and true or false");
    SpelNode rootAst = expr.getAST();
    OpOr operatorOr = (OpOr) rootAst;
    OpAnd operatorAnd = (OpAnd) operatorOr.getLeftOperand();
    SpelNode rightOrOperand = operatorOr.getRightOperand();

    // check position for final 'false'
    assertThat(rightOrOperand.getStartPosition()).isEqualTo(17);
    assertThat(rightOrOperand.getEndPosition()).isEqualTo(22);

    // check position for first 'true'
    assertThat(operatorAnd.getLeftOperand().getStartPosition()).isEqualTo(0);
    assertThat(operatorAnd.getLeftOperand().getEndPosition()).isEqualTo(4);

    // check position for second 'true'
    assertThat(operatorAnd.getRightOperand().getStartPosition()).isEqualTo(9);
    assertThat(operatorAnd.getRightOperand().getEndPosition()).isEqualTo(13);

    // check position for OperatorAnd
    assertThat(operatorAnd.getStartPosition()).isEqualTo(5);
    assertThat(operatorAnd.getEndPosition()).isEqualTo(8);

    // check position for OperatorOr
    assertThat(operatorOr.getStartPosition()).isEqualTo(14);
    assertThat(operatorOr.getEndPosition()).isEqualTo(16);
  }

  @Test
  void tokenKind() {
    TokenKind tk = TokenKind.NOT;
    assertThat(tk.hasPayload()).isFalse();
    assertThat(tk.toString()).isEqualTo("NOT(!)");

    tk = TokenKind.MINUS;
    assertThat(tk.hasPayload()).isFalse();
    assertThat(tk.toString()).isEqualTo("MINUS(-)");

    tk = TokenKind.LITERAL_STRING;
    assertThat(tk.toString()).isEqualTo("LITERAL_STRING");
    assertThat(tk.hasPayload()).isTrue();
  }

  @Test
  void token() {
    Token token = new Token(TokenKind.NOT, 0, 3);
    assertThat(token.kind).isEqualTo(TokenKind.NOT);
    assertThat(token.startPos).isEqualTo(0);
    assertThat(token.endPos).isEqualTo(3);
    assertThat(token.toString()).isEqualTo("[NOT(!)](0,3)");

    token = new Token(TokenKind.LITERAL_STRING, "abc".toCharArray(), 0, 3);
    assertThat(token.kind).isEqualTo(TokenKind.LITERAL_STRING);
    assertThat(token.startPos).isEqualTo(0);
    assertThat(token.endPos).isEqualTo(3);
    assertThat(token.toString()).isEqualTo("[LITERAL_STRING:abc](0,3)");
  }

  @Test
  void exceptions() {
    ExpressionException exprEx = new ExpressionException("test");
    assertThat(exprEx.getSimpleMessage()).isEqualTo("test");
    assertThat(exprEx.toDetailedString()).isEqualTo("test");
    assertThat(exprEx.getMessage()).isEqualTo("test");

    exprEx = new ExpressionException("wibble", "test");
    assertThat(exprEx.getSimpleMessage()).isEqualTo("test");
    assertThat(exprEx.toDetailedString()).isEqualTo("Expression [wibble]: test");
    assertThat(exprEx.getMessage()).isEqualTo("Expression [wibble]: test");

    exprEx = new ExpressionException("wibble", 3, "test");
    assertThat(exprEx.getSimpleMessage()).isEqualTo("test");
    assertThat(exprEx.toDetailedString()).isEqualTo("Expression [wibble] @3: test");
    assertThat(exprEx.getMessage()).isEqualTo("Expression [wibble] @3: test");
  }

  @Test
  void parseMethodsOnNumbers() {
    checkNumber("3.14.toString()", "3.14", String.class);
    checkNumber("3.toString()", "3", String.class);
  }

  @Test
  void numerics() {
    checkNumber("2", 2, Integer.class);
    checkNumber("22", 22, Integer.class);
    checkNumber("+22", 22, Integer.class);
    checkNumber("-22", -22, Integer.class);
    checkNumber("2L", 2L, Long.class);
    checkNumber("22l", 22L, Long.class);

    checkNumber("0x1", 1, Integer.class);
    checkNumber("0x1L", 1L, Long.class);
    checkNumber("0xa", 10, Integer.class);
    checkNumber("0xAL", 10L, Long.class);

    checkNumberError("0x", NOT_AN_INTEGER);
    checkNumberError("0xL", NOT_A_LONG);
    checkNumberError(".324", UNEXPECTED_DATA_AFTER_DOT);
    checkNumberError("3.4L", REAL_CANNOT_BE_LONG);

    checkNumber("3.5f", 3.5f, Float.class);
    checkNumber("1.2e3", 1.2e3d, Double.class);
    checkNumber("1.2e+3", 1.2e3d, Double.class);
    checkNumber("1.2e-3", 1.2e-3d, Double.class);
    checkNumber("1.2e3", 1.2e3d, Double.class);
    checkNumber("1e+3", 1e3d, Double.class);
  }

  private void checkNumber(String expression, Object value, Class<?> type) {
    try {
      SpelExpression expr = parser.parseRaw(expression);
      Object exprVal = expr.getValue();
      assertThat(exprVal).isEqualTo(value);
      assertThat(exprVal.getClass()).isEqualTo(type);
    }
    catch (Exception ex) {
      throw new AssertionError(ex.getMessage(), ex);
    }
  }

  private void checkNumberError(String expression, SpelMessage expectedMessage) {
    assertParseExceptionThrownBy(() -> parser.parseRaw(expression))
            .satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(expectedMessage));
  }

  private static ThrowableAssertAlternative<SpelParseException> assertParseExceptionThrownBy(ThrowableAssert.ThrowingCallable throwingCallable) {
    return assertThatExceptionOfType(SpelParseException.class).isThrownBy(throwingCallable);
  }

  private static void assertParseException(ThrowableAssert.ThrowingCallable throwingCallable, SpelMessage expectedMessage, int expectedPosition) {
    assertParseExceptionThrownBy(throwingCallable)
            .satisfies(parseExceptionRequirements(expectedMessage, expectedPosition));
  }

  private static <E extends SpelParseException> Consumer<E> parseExceptionRequirements(
          SpelMessage expectedMessage, int expectedPosition) {
    return ex -> {
      assertThat(ex.getMessageCode()).isEqualTo(expectedMessage);
      assertThat(ex.getPosition()).isEqualTo(expectedPosition);
      assertThat(ex.getMessage()).contains(ex.getExpressionString());
    };
  }

}
