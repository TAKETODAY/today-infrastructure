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

package cn.taketoday.context.expression;

import java.util.Map;

import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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
    this(new SpelExpressionParser());
  }

  /**
   * Create a new instance with the specified {@link SpelExpressionParser}.
   */
  protected CachedExpressionEvaluator(SpelExpressionParser parser) {
    this(parser, new DefaultParameterNameDiscoverer());
  }

  /**
   * Create a new instance with the specified {@link SpelExpressionParser}.
   */
  protected CachedExpressionEvaluator(SpelExpressionParser parser,
          ParameterNameDiscoverer parameterNameDiscoverer) {
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
  protected Expression getExpression(
          Map<ExpressionKey, Expression> cache, AnnotatedElementKey elementKey, String expression) {
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
      return this.element + " with expression \"" + this.expression + "\"";
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
