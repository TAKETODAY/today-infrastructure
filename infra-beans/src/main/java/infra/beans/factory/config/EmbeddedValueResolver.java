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

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import infra.core.StringValueResolver;
import infra.util.ObjectUtils;

/**
 * {@link StringValueResolver} adapter for resolving placeholders and
 * expressions against a {@link ConfigurableBeanFactory}.
 *
 * <p>Note that this adapter resolves expressions as well, in contrast
 * to the {@link ConfigurableBeanFactory#resolveEmbeddedValue} method.
 * The {@link BeanExpressionContext} used is for the plain bean factory,
 * with no scope specified for any contextual objects to access.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurableBeanFactory#resolveEmbeddedValue(String)
 * @see ConfigurableBeanFactory#getBeanExpressionResolver()
 * @see BeanExpressionContext
 * @since 4.0 2021/12/7 11:24
 */
public class EmbeddedValueResolver implements StringValueResolver {
  private final ExpressionEvaluator expressionEvaluator;

  public EmbeddedValueResolver(ConfigurableBeanFactory beanFactory) {
    this.expressionEvaluator = ExpressionEvaluator.from(beanFactory);
  }

  @Override
  @Nullable
  public String resolveStringValue(@Nullable String strVal) {
    return ObjectUtils.toString(expressionEvaluator.evaluate(strVal));
  }

  /**
   * contains placeholder or EL expressions
   *
   * @param expr expression
   */
  public static boolean isEmbedded(@Nullable String expr) {
    return expr != null && ((expr.startsWith("#{") || expr.startsWith("${")) && expr.endsWith("}"));
  }

}
