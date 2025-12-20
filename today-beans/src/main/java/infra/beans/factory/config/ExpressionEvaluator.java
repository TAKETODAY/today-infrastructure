/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryUtils;
import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * Utility class for evaluating expressions within the context of a bean factory.
 * Provides methods to resolve and evaluate expressions, typically used for
 * processing placeholder values and other expression-based configurations.
 *
 * @author TODAY 2021/4/8 19:42
 * @since 3.0
 */
public class ExpressionEvaluator {

  public static final String BEAN_NAME = "expressionEvaluator";

  // @since 4.0

  private final BeanExpressionContext exprContext;

  private final ConfigurableBeanFactory beanFactory;

  public ExpressionEvaluator(ConfigurableBeanFactory beanFactory) {
    this.exprContext = new BeanExpressionContext(beanFactory, null);
    this.beanFactory = beanFactory;
  }

  public @Nullable Object evaluate(@Nullable String expression) {
    expression = beanFactory.resolveEmbeddedValue(expression);
    if (expression != null) {
      BeanExpressionResolver exprResolver = beanFactory.getBeanExpressionResolver();
      if (exprResolver != null) {
        return exprResolver.evaluate(expression, exprContext);
      }
    }
    return expression;
  }

  @SuppressWarnings("unchecked")
  public <T> @Nullable T evaluate(@Nullable String expression, Class<T> expectedType) {
    expression = beanFactory.resolveEmbeddedValue(expression);
    Object evaluated;
    if (expression != null) {
      BeanExpressionResolver exprResolver = beanFactory.getBeanExpressionResolver();
      if (exprResolver != null) {
        evaluated = exprResolver.evaluate(expression, exprContext);
      }
      else
        evaluated = expression;
    }
    else {
      return null;
    }
    if (ClassUtils.isAssignableValue(expectedType, evaluated)) {
      return (T) evaluated;
    }
    return beanFactory.getTypeConverter().convertIfNecessary(evaluated, expectedType);
  }

  public static ExpressionEvaluator from(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "beanFactory is required");
    ExpressionEvaluator evaluator = BeanFactoryUtils.findLocal(beanFactory, BEAN_NAME, ExpressionEvaluator.class);
    if (evaluator == null) {
      synchronized(beanFactory) {
        evaluator = BeanFactoryUtils.findLocal(beanFactory, BEAN_NAME, ExpressionEvaluator.class);
        if (evaluator == null) {
          evaluator = new ExpressionEvaluator(beanFactory.unwrap(ConfigurableBeanFactory.class));
          beanFactory.unwrap(ConfigurableBeanFactory.class)
                  .registerSingleton(BEAN_NAME, evaluator);
        }
      }
    }
    return evaluator;
  }

}
