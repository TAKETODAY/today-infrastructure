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

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.core.ParameterNameDiscoverer;
import infra.expression.Expression;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.lang.Assert;

/**
 * Shared utility class used to evaluate and cache EL expressions that
 * are defined on {@link java.lang.reflect.AnnotatedElement}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 16:59
 */
public abstract class CachedExpressionEvaluator {

  protected final SpelExpressionParser parser;

  /**
   * a shared parameter name discoverer which caches data internally.
   */
  protected final ParameterNameDiscoverer parameterNameDiscoverer;

  /**
   * Create a new instance with a default {@link SpelExpressionParser}.
   */
  protected CachedExpressionEvaluator() {
    this(SpelExpressionParser.INSTANCE);
  }

  /**
   * Create a new instance with the specified {@link SpelExpressionParser}.
   */
  protected CachedExpressionEvaluator(SpelExpressionParser parser) {
    this(parser, ParameterNameDiscoverer.getSharedInstance());
  }

  /**
   * Create a new instance with the specified {@link SpelExpressionParser}.
   */
  protected CachedExpressionEvaluator(SpelExpressionParser parser, ParameterNameDiscoverer parameterNameDiscoverer) {
    Assert.notNull(parser, "SpelExpressionParser is required");
    Assert.notNull(parameterNameDiscoverer, "ParameterNameDiscoverer is required");
    this.parser = parser;
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  /**
   * Return the {@link Expression} for the specified SpEL value
   * <p>{@link #parseExpression(String) Parse the expression} if it hasn't been already.
   *
   * @param cache the cache to use
   * @param elementKey the element on which the expression is defined
   * @param expression the expression to parse
   */
  protected Expression getExpression(Map<ExpressionKey, Expression> cache, AnnotatedElementKey elementKey, String expression) {
    ExpressionKey expressionKey = createKey(elementKey, expression);
    Expression expr = cache.get(expressionKey);
    if (expr == null) {
      expr = parseExpression(expression);
      cache.put(expressionKey, expr);
    }
    return expr;
  }

  /**
   * Parse the specified {@code expression}.
   *
   * @param expression the expression to parse
   */
  protected Expression parseExpression(String expression) {
    return parser.parseExpression(expression);
  }

  private ExpressionKey createKey(AnnotatedElementKey elementKey, String expression) {
    return new ExpressionKey(elementKey, expression);
  }

  /**
   * An expression key.
   */
  protected static class ExpressionKey implements Comparable<ExpressionKey> {

    private final String expression;

    private final AnnotatedElementKey element;

    protected ExpressionKey(AnnotatedElementKey element, String expression) {
      Assert.notNull(element, "AnnotatedElementKey is required");
      Assert.notNull(expression, "Expression is required");
      this.element = element;
      this.expression = expression;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ExpressionKey otherKey)) {
        return false;
      }
      return element.equals(otherKey.element)
              && expression.equals(otherKey.expression);
    }

    @Override
    public int hashCode() {
      return this.element.hashCode() * 29 + this.expression.hashCode();
    }

    @Override
    public String toString() {
      return "%s with expression \"%s\"".formatted(this.element, this.expression);
    }

    @Override
    public int compareTo(ExpressionKey other) {
      int result = this.element.toString().compareTo(other.element.toString());
      if (result == 0) {
        result = this.expression.compareTo(other.expression);
      }
      return result;
    }
  }

}
