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

import cn.taketoday.expression.BeanPropertyExpressionResolver;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 22:10
 */
public class RootObjectExpressionResolver extends BeanPropertyExpressionResolver {
  private final Object rootObject;

  public RootObjectExpressionResolver(Object rootObject) {
    this(rootObject, false, false);
  }

  public RootObjectExpressionResolver(Object rootObject, boolean ignoreUnknownProperty) {
    this(rootObject, false, ignoreUnknownProperty);
  }

  public RootObjectExpressionResolver(Object rootObject, boolean isReadOnly, boolean ignoreUnknownProperty) {
    super(isReadOnly, ignoreUnknownProperty);
    Assert.notNull(rootObject, "'rootObject' is required");
    this.rootObject = rootObject;
  }

  @Override
  public Object getValue(
          ExpressionContext context, @Nullable Object base, Object propertyName) {
    if (base == null && propertyName != null) {
      return super.getValue(context, rootObject, propertyName);
    }
    return null;
  }

  @Override
  public Class<?> getType(ExpressionContext context, Object base, Object property) {
    if (base == null && property != null) {
      return super.getType(context, rootObject, property);
    }
    return null;
  }

  @Override
  public void setValue(
          ExpressionContext context, Object base, Object property, Object value) {
    if (base == null && property != null) {
      super.setValue(context, rootObject, property, value);
    }
  }

  @Override
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    if (base != null || property == null) {
      return false;
    }
    context.setPropertyResolved(true);
    return super.isReadOnly(context, rootObject, property);
  }

}
