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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ParseException;
import cn.taketoday.expression.ParserContext;
import cn.taketoday.expression.common.CompositeStringExpression;
import cn.taketoday.expression.common.TemplateParserContext;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public class TemplateExpressionParsingTests extends AbstractExpressionTests {

  static final ParserContext DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT = new TemplateParserContext("${", "}");

  private final SpelExpressionParser parser = new SpelExpressionParser();

  @Test
  void nullTemplateExpressionIsRejected() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> parser.parseExpression(null, DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT))
            .withMessage("'expressionString' must not be null");
  }

  @Test
  void parsingSimpleTemplateExpression01() {
    Expression expr = parser.parseExpression("hello ${'world'}", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    Object o = expr.getValue();
    assertThat(o.toString()).isEqualTo("hello world");
  }

  @Test
  void parsingSimpleTemplateExpression02() {
    Expression expr = parser.parseExpression("hello ${'to'} you", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    Object o = expr.getValue();
    assertThat(o.toString()).isEqualTo("hello to you");
  }

  @Test
  void parsingSimpleTemplateExpression03() {
    Expression expr = parser.parseExpression("The quick ${'brown'} fox jumped over the ${'lazy'} dog",
            DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    Object o = expr.getValue();
    assertThat(o.toString()).isEqualTo("The quick brown fox jumped over the lazy dog");
  }

  @Test
  void parsingSimpleTemplateExpression04() {
    Expression expr = parser.parseExpression("${'hello'} world", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    Object o = expr.getValue();
    assertThat(o.toString()).isEqualTo("hello world");

    expr = parser.parseExpression("", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    o = expr.getValue();
    assertThat(o.toString()).isEmpty();

    expr = parser.parseExpression("abc", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    o = expr.getValue();
    assertThat(o.toString()).isEqualTo("abc");

    expr = parser.parseExpression("abc", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    o = expr.getValue((Object) null);
    assertThat(o.toString()).isEqualTo("abc");
  }

  @Test
  void compositeStringExpression() {
    Expression ex = parser.parseExpression("hello ${'world'}", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    assertThat(ex.getValue()).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getValue(String.class)).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getValue((Object) null, String.class)).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getValue(new Rooty())).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getValue(new Rooty(), String.class)).isInstanceOf(String.class).isEqualTo("hello world");

    EvaluationContext ctx = new StandardEvaluationContext();
    assertThat(ex.getValue(ctx)).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getValue(ctx, String.class)).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getValue(ctx, null, String.class)).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getValue(ctx, new Rooty())).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getValue(ctx, new Rooty(), String.class)).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getValue(ctx, new Rooty(), String.class)).isInstanceOf(String.class).isEqualTo("hello world");
    assertThat(ex.getExpressionString()).isEqualTo("hello ${'world'}");
    assertThat(ex.isWritable(new StandardEvaluationContext())).isFalse();
    assertThat(ex.isWritable(new Rooty())).isFalse();
    assertThat(ex.isWritable(new StandardEvaluationContext(), new Rooty())).isFalse();

    assertThat(ex.getValueType()).isEqualTo(String.class);
    assertThat(ex.getValueType(ctx)).isEqualTo(String.class);
    assertThat(ex.getValueTypeDescriptor().getType()).isEqualTo(String.class);
    assertThat(ex.getValueTypeDescriptor(ctx).getType()).isEqualTo(String.class);
    assertThat(ex.getValueType(new Rooty())).isEqualTo(String.class);
    assertThat(ex.getValueType(ctx, new Rooty())).isEqualTo(String.class);
    assertThat(ex.getValueTypeDescriptor(new Rooty()).getType()).isEqualTo(String.class);
    assertThat(ex.getValueTypeDescriptor(ctx, new Rooty()).getType()).isEqualTo(String.class);
    assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
            ex.setValue(ctx, null));
    assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
            ex.setValue((Object) null, null));
    assertThatExceptionOfType(EvaluationException.class).isThrownBy(() ->
            ex.setValue(ctx, null, null));
  }

  static class Rooty { }

  @Test
  void nestedExpressions() {
    // treat the nested ${..} as a part of the expression
    Expression ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} world", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    String s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(), String.class);
    assertThat(s).isEqualTo("hello 4 world");

    // not a useful expression but tests nested expression syntax that clashes with template prefix/suffix
    ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1]==3]} world", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    assertThat(ex.getClass()).isEqualTo(CompositeStringExpression.class);
    CompositeStringExpression cse = (CompositeStringExpression) ex;
    Expression[] exprs = cse.getExpressions();
    assertThat(exprs).hasSize(3);
    assertThat(exprs[1].getExpressionString()).isEqualTo("listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1]==3]");
    s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(), String.class);
    assertThat(s).isEqualTo("hello  world");

    ex = parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} ${listOfNumbersUpToTen.$[#this>5]} world", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(), String.class);
    assertThat(s).isEqualTo("hello 4 10 world");

    assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
                    parser.parseExpression("hello ${listOfNumbersUpToTen.$[#this<5]} ${listOfNumbersUpToTen.$[#this>5] world", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT))
            .satisfies(pex -> assertThat(pex.getSimpleMessage()).isEqualTo("No ending suffix '}' for expression starting at character 41: ${listOfNumbersUpToTen.$[#this>5] world"));

    assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
                    parser.parseExpression("hello ${listOfNumbersUpToTen.$[#root.listOfNumbersUpToTen.$[#this%2==1==3]} world", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT))
            .satisfies(pex -> assertThat(pex.getSimpleMessage()).isEqualTo("Found closing '}' at position 74 but most recent opening is '[' at position 30"));
  }

  @Test
  void clashingWithSuffixes() {
    // Just wanting to use the prefix or suffix within the template:
    Expression ex = parser.parseExpression("hello ${3+4} world", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    String s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(), String.class);
    assertThat(s).isEqualTo("hello 7 world");

    ex = parser.parseExpression("hello ${3+4} wo${'${'}rld", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(), String.class);
    assertThat(s).isEqualTo("hello 7 wo${rld");

    ex = parser.parseExpression("hello ${3+4} wo}rld", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT);
    s = ex.getValue(TestScenarioCreator.getTestEvaluationContext(), String.class);
    assertThat(s).isEqualTo("hello 7 wo}rld");
  }

  @Test
  void parsingNormalExpressionThroughTemplateParser() {
    Expression expr = parser.parseExpression("1+2+3");
    assertThat(expr.getValue()).isEqualTo(6);
  }

  @Test
  void errorCases() {
    assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
                    parser.parseExpression("hello ${'world'", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT))
            .satisfies(pex -> {
              assertThat(pex.getSimpleMessage()).isEqualTo("No ending suffix '}' for expression starting at character 6: ${'world'");
              assertThat(pex.getExpressionString()).isEqualTo("hello ${'world'");
            });
    assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
                    parser.parseExpression("hello ${'wibble'${'world'}", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT))
            .satisfies(pex -> assertThat(pex.getSimpleMessage()).isEqualTo("No ending suffix '}' for expression starting at character 6: ${'wibble'${'world'}"));
    assertThatExceptionOfType(ParseException.class).isThrownBy(() ->
                    parser.parseExpression("hello ${} world", DOLLAR_SIGN_TEMPLATE_PARSER_CONTEXT))
            .satisfies(pex -> assertThat(pex.getSimpleMessage()).isEqualTo("No expression defined within delimiter '${}' at character 6"));
  }

  @Test
  void templateParserContext() {
    TemplateParserContext tpc = new TemplateParserContext("abc", "def");
    assertThat(tpc.getExpressionPrefix()).isEqualTo("abc");
    assertThat(tpc.getExpressionSuffix()).isEqualTo("def");
    assertThat(tpc.isTemplate()).isTrue();

    tpc = new TemplateParserContext();
    assertThat(tpc.getExpressionPrefix()).isEqualTo("#{");
    assertThat(tpc.getExpressionSuffix()).isEqualTo("}");
    assertThat(tpc.isTemplate()).isTrue();

    ParserContext pc = ParserContext.TEMPLATE_EXPRESSION;
    assertThat(pc.getExpressionPrefix()).isEqualTo("#{");
    assertThat(pc.getExpressionSuffix()).isEqualTo("}");
    assertThat(pc.isTemplate()).isTrue();
  }

}
