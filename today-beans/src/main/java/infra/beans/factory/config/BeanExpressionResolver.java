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
