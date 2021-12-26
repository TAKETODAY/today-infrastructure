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

import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.expression.BeanPropertyExpressionResolver;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.expression.PropertyNotWritableException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 22:10
 */
public class RootObjectExpressionResolver extends ExpressionResolver {
  private final Object rootObject;
  private final boolean isReadOnly;

  public RootObjectExpressionResolver(Object rootObject) {
    this(rootObject, false);
  }

  public RootObjectExpressionResolver(Object rootObject, boolean isReadOnly) {
    Assert.notNull(rootObject, "'rootObject' is required");
    this.rootObject = rootObject;
    this.isReadOnly = isReadOnly;
  }

  @Override
  public Object getValue(
          ExpressionContext context, @Nullable Object base, Object propertyName) {
    if (base == null && propertyName != null) {
      BeanProperty property = getProperty(propertyName);
      try {
        Object value = property.getValue(rootObject);
        context.setPropertyResolved(rootObject, property);
        return value;
      }
      catch (Exception ex) {
        throw new ExpressionException(
                "Can't get property: '" + propertyName + "' from '" + rootObject.getClass() + "'", ex);
      }
    }
    return null;
  }

  @Override
  public Class<?> getType(ExpressionContext context, Object base, Object property) {
    if (base == null && property != null) {
      BeanProperty beanProperty = getProperty(property);
      context.setPropertyResolved(true);
      return beanProperty.getType();
    }
    return null;
  }

  private BeanProperty getProperty(Object property) {
    return BeanPropertyExpressionResolver.getProperty(rootObject, property);
  }

  @Override
  public void setValue(
          ExpressionContext context, Object base, Object property, Object value) {
    if (base == null && property != null) {
      if (isReadOnly) {
        throw new PropertyNotWritableException(
                "The ExpressionResolver for the class '" + rootObject.getClass().getName() + "' is not writable.");
      }

      BeanProperty beanProperty = getProperty(property);
      try {
        beanProperty.setValue(rootObject, value);
        context.setPropertyResolved(rootObject, property);
      }
      catch (Exception ex) {
        StringBuilder message = new StringBuilder("Can't set property '")//
                .append(property)//
                .append("' on class '")//
                .append(rootObject.getClass().getName())//
                .append("' to value '")//
                .append(value)//
                .append("'.");
        throw new ExpressionException(message.toString(), ex);
      }
    }
  }

  @Override
  public boolean isReadOnly(ExpressionContext context, Object base, Object property) {
    if (base != null || property == null) {
      return false;
    }
    context.setPropertyResolved(true);
    return isReadOnly;
  }

}
