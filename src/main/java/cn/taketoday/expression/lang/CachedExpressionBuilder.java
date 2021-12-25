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

package cn.taketoday.expression.lang;

import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.FunctionMapper;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.expression.parser.Node;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.ConcurrentReferenceHashMap;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 22:55
 */
public class CachedExpressionBuilder extends ExpressionBuilder {
  public static final String CACHE_SIZE_PROP = "expression.cache.size";

  private static final ConcurrentReferenceHashMap<String, Node> EXPRESSION_CACHE
          = new ConcurrentReferenceHashMap<>(TodayStrategies.getInt(CACHE_SIZE_PROP, 256));

  public CachedExpressionBuilder(String expression, ExpressionContext ctx) throws ExpressionException {
    super(expression, ctx);
  }

  public CachedExpressionBuilder(String expression, @Nullable FunctionMapper ctxFn, @Nullable VariableMapper ctxVar)
          throws ExpressionException {
    super(expression, ctxFn, ctxVar);
  }

  @Override
  protected Node createNode(String expression) {
    return getNode(expression);
  }

  public static Node getNode(final String expr) throws ExpressionException {
    if (expr == null) {
      throw new ExpressionException("Expression cannot be null");
    }
    Node node = EXPRESSION_CACHE.get(expr);
    if (node == null) {
      node = parse(expr);
      EXPRESSION_CACHE.put(expr, node);
    }
    return node;
  }

}
