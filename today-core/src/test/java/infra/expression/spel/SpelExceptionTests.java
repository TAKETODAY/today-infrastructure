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

package infra.expression.spel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import infra.expression.Expression;
import infra.expression.ExpressionParser;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * SpelEvaluationException tests .
 *
 * @author Juergen Hoeller
 * @author DJ Kulkarni
 */
public class SpelExceptionTests {

  @Test
  public void spelExpressionMapNullVariables() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression spelExpression = parser.parseExpression("#aMap.containsKey('one')");
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
            spelExpression::getValue);
  }

  @Test
  public void spelExpressionMapIndexAccessNullVariables() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression spelExpression = parser.parseExpression("#aMap['one'] eq 1");
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
            spelExpression::getValue);
  }

  @Test
  @SuppressWarnings("serial")
  public void spelExpressionMapWithVariables() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression spelExpression = parser.parseExpression("#aMap['one'] eq 1");
    StandardEvaluationContext ctx = new StandardEvaluationContext();
    ctx.setVariables(new HashMap<String, Object>() {
      {
        put("aMap", new HashMap<String, Integer>() {
          {
            put("one", 1);
            put("two", 2);
            put("three", 3);
          }
        });

      }
    });
    boolean result = spelExpression.getValue(ctx, Boolean.class);
    assertThat(result).isTrue();

  }

  @Test
  public void spelExpressionListNullVariables() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression spelExpression = parser.parseExpression("#aList.contains('one')");
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
            spelExpression::getValue);
  }

  @Test
  public void spelExpressionListIndexAccessNullVariables() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression spelExpression = parser.parseExpression("#aList[0] eq 'one'");
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
            spelExpression::getValue);
  }

  @Test
  @SuppressWarnings("serial")
  public void spelExpressionListWithVariables() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression spelExpression = parser.parseExpression("#aList.contains('one')");
    StandardEvaluationContext ctx = new StandardEvaluationContext();
    ctx.setVariables(new HashMap<String, Object>() {
      {
        put("aList", new ArrayList<String>() {
          {
            add("one");
            add("two");
            add("three");
          }
        });

      }
    });
    boolean result = spelExpression.getValue(ctx, Boolean.class);
    assertThat(result).isTrue();
  }

  @Test
  @SuppressWarnings("serial")
  public void spelExpressionListIndexAccessWithVariables() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression spelExpression = parser.parseExpression("#aList[0] eq 'one'");
    StandardEvaluationContext ctx = new StandardEvaluationContext();
    ctx.setVariables(new HashMap<String, Object>() {
      {
        put("aList", new ArrayList<String>() {
          {
            add("one");
            add("two");
            add("three");
          }
        });

      }
    });
    boolean result = spelExpression.getValue(ctx, Boolean.class);
    assertThat(result).isTrue();
  }

  @Test
  public void spelExpressionArrayIndexAccessNullVariables() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression spelExpression = parser.parseExpression("#anArray[0] eq 1");
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(
            spelExpression::getValue);
  }

  @Test
  @SuppressWarnings("serial")
  public void spelExpressionArrayWithVariables() {
    ExpressionParser parser = new SpelExpressionParser();
    Expression spelExpression = parser.parseExpression("#anArray[0] eq 1");
    StandardEvaluationContext ctx = new StandardEvaluationContext();
    ctx.setVariables(new HashMap<String, Object>() {
      {
        put("anArray", new int[] { 1, 2, 3 });
      }
    });
    boolean result = spelExpression.getValue(ctx, Boolean.class);
    assertThat(result).isTrue();
  }

}
