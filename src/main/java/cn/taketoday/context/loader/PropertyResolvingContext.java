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

package cn.taketoday.context.loader;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.context.annotation.PropsReader;

/**
 * @author TODAY 2021/10/3 21:58
 * @since 4.0
 */
public final class PropertyResolvingContext {
  private final PropsReader propsReader;
  private final ApplicationContext context;
  private final ExpressionEvaluator expressionEvaluator;

  public PropertyResolvingContext(ApplicationContext context) {
    this(context, new PropsReader(context.getEnvironment()));
  }

  public PropertyResolvingContext(ApplicationContext context, PropsReader propsReader) {
    this.context = context;
    this.propsReader = propsReader;
    this.expressionEvaluator = context.getExpressionEvaluator();
  }

  public ApplicationContext getContext() {
    return context;
  }

  public PropsReader getPropsReader() {
    return propsReader;
  }

  public ExpressionEvaluator getExpressionEvaluator() {
    return expressionEvaluator;
  }

}
