/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.expression;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.expression.Expression;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
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
    assertThat(expression.getValue()).asInstanceOf(InstanceOfAssertFactories.BOOLEAN).isTrue();
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
    verify(expressionEvaluator.parser, times(1)).parseExpression(expression);
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
