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

package cn.taketoday.beans.factory.config;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.BeanExpressionContext;
import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for resolving a value through evaluating it
 * as an expression, if applicable.
 *
 * <p>A raw {@link BeanFactory} does not
 * contain a default implementation of this strategy. However,
 * {@link cn.taketoday.context.ApplicationContext} implementations
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
