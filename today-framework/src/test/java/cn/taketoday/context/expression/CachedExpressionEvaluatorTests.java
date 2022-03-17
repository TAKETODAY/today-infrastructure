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

package cn.taketoday.context.expression;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.expression.Expression;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 17:19
 */
class CachedExpressionEvaluatorTests {

  private final TestExpressionEvaluator expressionEvaluator = new TestExpressionEvaluator();

  @Test
  public void parseNewExpression() {
    Method method = ReflectionUtils.findMethod(getClass(), "toString");
    Expression expression = expressionEvaluator.getTestExpression("true", method, getClass());
    hasParsedExpression("true");
    assertThat(expression.getValue()).asInstanceOf(BOOLEAN).isTrue();
    assertThat(expressionEvaluator.testCache.size()).as("Expression should be in cache").isEqualTo(1);
  }

  @Test
  public void cacheExpression() {
    Method method = ReflectionUtils.findMethod(getClass(), "toString");

    expressionEvaluator.getTestExpression("true", method, getClass());
    expressionEvaluator.getTestExpression("true", method, getClass());
    expressionEvaluator.getTestExpression("true", method, getClass());
    hasParsedExpression("true");
    assertThat(expressionEvaluator.testCache.size()).as("Only one expression should be in cache").isEqualTo(1);
  }

  @Test
  public void cacheExpressionBasedOnConcreteType() {
    Method method = ReflectionUtils.findMethod(getClass(), "toString");
    expressionEvaluator.getTestExpression("true", method, getClass());
    expressionEvaluator.getTestExpression("true", method, Object.class);
    assertThat(expressionEvaluator.testCache.size()).as("Cached expression should be based on type").isEqualTo(2);
  }

  private void hasParsedExpression(String expression) {
    verify(expressionEvaluator.getParser(), times(1)).parseExpression(expression);
  }

  private static class TestExpressionEvaluator extends CachedExpressionEvaluator {

    private final Map<ExpressionKey, Expression> testCache = new ConcurrentHashMap<>();

    public TestExpressionEvaluator() {
      super(mockSpelExpressionParser());
    }

    public Expression getTestExpression(String expression, Method method, Class<?> type) {
      return getExpression(this.testCache, new AnnotatedElementKey(method, type), expression);
    }

    private static SpelExpressionParser mockSpelExpressionParser() {
      SpelExpressionParser parser = new SpelExpressionParser();
      return spy(parser);
    }
  }

}