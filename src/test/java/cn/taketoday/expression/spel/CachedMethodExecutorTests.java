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

import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ExpressionParser;
import cn.taketoday.expression.spel.ast.MethodReference;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for caching in {@link MethodReference} (SPR-10657).
 *
 * @author Oliver Becker
 */
public class CachedMethodExecutorTests {

  private final ExpressionParser parser = new SpelExpressionParser();

  private final StandardEvaluationContext context = new StandardEvaluationContext(new RootObject());

  @Test
  public void testCachedExecutionForParameters() {
    Expression expression = this.parser.parseExpression("echo(#var)");

    assertMethodExecution(expression, 42, "int: 42");
    assertMethodExecution(expression, 42, "int: 42");
    assertMethodExecution(expression, "Deep Thought", "String: Deep Thought");
    assertMethodExecution(expression, 42, "int: 42");
  }

  @Test
  public void testCachedExecutionForTarget() {
    Expression expression = this.parser.parseExpression("#var.echo(42)");

    assertMethodExecution(expression, new RootObject(), "int: 42");
    assertMethodExecution(expression, new RootObject(), "int: 42");
    assertMethodExecution(expression, new BaseObject(), "String: 42");
    assertMethodExecution(expression, new RootObject(), "int: 42");
  }

  private void assertMethodExecution(Expression expression, Object var, String expected) {
    this.context.setVariable("var", var);
    assertThat(expression.getValue(this.context)).isEqualTo(expected);
  }

  public static class BaseObject {

    public String echo(String value) {
      return "String: " + value;
    }
  }

  public static class RootObject extends BaseObject {

    public String echo(int value) {
      return "int: " + value;
    }
  }

}
