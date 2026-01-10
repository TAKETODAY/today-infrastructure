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

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;

/**
 * Strategy interface for resolving a value through evaluating it
 * as an expression, if applicable.
 *
 * <p>A raw {@link BeanFactory} does not
 * contain a default implementation of this strategy. However,
 * {@link infra.context.ApplicationContext} implementations
 * will provide expression support out of the box.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface BeanExpressionResolver {

  /**
   * Evaluate the given value as an expression, if applicable;
   * return the value as-is otherwise.
   *
   * @param value the value to check
   * @param evalContext the evaluation context
   * @return the resolved value (potentially the given value as-is)
   * @throws BeansException if evaluation failed
   */
  @Nullable
  Object evaluate(@Nullable String value, BeanExpressionContext evalContext) throws BeansException;

}
