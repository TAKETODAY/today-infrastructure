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

import cn.taketoday.beans.factory.config.BeanExpressionContext;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.ExpressionEvaluator;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

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
  public String resolveStringValue(String strVal) {
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
