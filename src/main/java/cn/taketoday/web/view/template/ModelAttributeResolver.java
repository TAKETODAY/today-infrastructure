/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.view.template;

import cn.taketoday.lang.Assert;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.web.view.Model;

/**
 * For the {@link Model} attribute
 *
 * @author TODAY 2021/4/15 20:49
 * @since 3.0
 */
public class ModelAttributeResolver extends ExpressionResolver {
  private final Model model;

  public ModelAttributeResolver(Model model) {
    Assert.notNull(model, "Model cannot be null");
    this.model = model;
  }

  @Override
  public Object getValue(ExpressionContext context, Object base, Object property) {
    if (base == null && property instanceof String) {
      final Model model = this.model;
      if (model.containsAttribute((String) property)) {
        context.setPropertyResolved(null, property);
        return model.getAttribute((String) property);
      }
    }
    return null;
  }

  @Override
  public void setValue(ExpressionContext elContext, Object base, Object property, Object value) {
    if (base == null && property instanceof String) {
      final String beanName = (String) property;
      final Model model = this.model;
      if (model.containsAttribute(beanName)) {
        model.setAttribute(beanName, value);
        elContext.setPropertyResolved(base, property);
      }
    }
  }

  @Override
  public Class<?> getType(ExpressionContext elContext, Object base, Object property) {
    if (base == null && property instanceof String) {
      final Model model = this.model;
      if (model.containsAttribute((String) property)) {
        elContext.setPropertyResolved(true);
        return model.getAttribute((String) property).getClass();
      }
    }
    return null;
  }

  @Override
  public boolean isReadOnly(ExpressionContext elContext, Object base, Object property) {
    if (base == null && property instanceof String && model.containsAttribute((String) property)) {
      elContext.setPropertyResolved(true);
    }
    return false;
  }
}
