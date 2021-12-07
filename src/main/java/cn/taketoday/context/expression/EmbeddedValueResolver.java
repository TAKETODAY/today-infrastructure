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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link StringValueResolver} adapter for resolving placeholders and
 * expressions
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/7 11:24
 */
public class EmbeddedValueResolver implements StringValueResolver {
  private final ExpressionEvaluator expressionEvaluator;

  public EmbeddedValueResolver(ApplicationContext applicationContext) {
    this.expressionEvaluator = applicationContext.getExpressionEvaluator();
  }

  public EmbeddedValueResolver(ExpressionEvaluator expressionEvaluator) {
    Assert.notNull(expressionEvaluator, "ExpressionEvaluator is required");
    this.expressionEvaluator = expressionEvaluator;
  }

  @Nullable
  @Override
  public String resolveStringValue(String strVal) {
    return expressionEvaluator.evaluate(strVal, String.class);
  }
}
