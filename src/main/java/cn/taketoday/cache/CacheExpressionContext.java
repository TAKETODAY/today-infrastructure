/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.cache;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.expression.BeanNameExpressionResolver;
import cn.taketoday.expression.CompositeExpressionResolver;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.expression.FunctionMapper;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.expression.lang.LocalBeanNameResolver;

/**
 * @author TODAY <br>
 * 2019-02-17 20:40
 */
public class CacheExpressionContext extends ExpressionContext {

  private ExpressionResolver elResolver;
  private final Map<String, Object> beans;
  private final StandardExpressionContext parent;

  public CacheExpressionContext(StandardExpressionContext parent) {
    this(parent, new HashMap<>(4));
  }

  public CacheExpressionContext(StandardExpressionContext parent, Map<String, Object> beans) {
    this.beans = beans;
    this.parent = parent;
  }

  @Override
  public ExpressionResolver getResolver() {
    final ExpressionResolver ret = this.elResolver;
    if (ret == null) {
      return this.elResolver = new CompositeExpressionResolver(
              new BeanNameExpressionResolver(new LocalBeanNameResolver(beans)),
              parent.getResolver()
      );
    }
    return ret;
  }

  /**
   * Add a bean to this context
   *
   * @param name
   *         bean name
   * @param bean
   *         bean instance
   */
  public void putBean(final String name, final Object bean) {
    beans.put(name, bean);
  }

  @Override
  public FunctionMapper getFunctionMapper() {
    return this.parent.getFunctionMapper();
  }

  @Override
  public VariableMapper getVariableMapper() {
    return this.parent.getVariableMapper();
  }

  @Override
  public void setPropertyResolved(Object base, Object property) {
    setPropertyResolved(true);
  }

}
