/*
 * Copyright 2017 - 2026 the TODAY authors.
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
