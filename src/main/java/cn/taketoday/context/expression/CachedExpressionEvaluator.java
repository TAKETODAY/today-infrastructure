/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import cn.taketoday.expression.ValueExpression;
import cn.taketoday.expression.lang.ExpressionBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Shared utility class used to evaluate and cache EL expressions that
 * are defined on {@link java.lang.reflect.AnnotatedElement}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 16:59
 */
public abstract class CachedExpressionEvaluator {
  protected final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  /**
   * Return the {@link ValueExpression} for the specified EL value
   * <p>{@link #parseExpression(String) Parse the expression} if it hasn't been already.
   *
   * @param cache the cache to use
   * @param elementKey the element on which the expression is defined
   * @param expression the expression to parse
   */
  protected ValueExpression getExpression(
          Map<ExpressionKey, ValueExpression> cache,
          Object elementKey, String expression) {

    ExpressionKey expressionKey = createKey(elementKey, expression);
    ValueExpression expr = cache.get(expressionKey);
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
  private ValueExpression parseExpression(String expression) {
    return new ExpressionBuilder(expression, null, null).build(null);// FIXME ctxFn
  }

  private ExpressionKey createKey(Object elementKey, String expression) {
    return new ExpressionKey(elementKey, expression);
  }

  public ParameterNameDiscoverer getParameterNameDiscoverer() {
    return parameterNameDiscoverer;
  }

  /**
   * An expression key.
   */
  protected static class ExpressionKey implements Comparable<ExpressionKey> {

    private final Object elementKey;
    private final String expression;

    protected ExpressionKey(Object elementKey, String expression) {
      Assert.notNull(elementKey, "AnnotatedElementKey must not be null");
      Assert.notNull(expression, "Expression must not be null");
      this.elementKey = elementKey;
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
      return (this.elementKey.equals(otherKey.elementKey) &&
              ObjectUtils.nullSafeEquals(this.expression, otherKey.expression));
    }

    @Override
    public int hashCode() {
      return this.elementKey.hashCode() * 29 + this.expression.hashCode();
    }

    @Override
    public String toString() {
      return this.elementKey + " with expression \"" + this.expression + "\"";
    }

    @Override
    public int compareTo(ExpressionKey other) {
      int result = this.elementKey.toString().compareTo(other.elementKey.toString());
      if (result == 0) {
        result = this.expression.compareTo(other.expression);
      }
      return result;
    }
  }

}
