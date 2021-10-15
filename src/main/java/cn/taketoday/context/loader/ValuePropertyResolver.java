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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.loader;

import cn.taketoday.beans.factory.DefaultPropertySetter;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.context.expression.ExpressionInfo;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;
import cn.taketoday.lang.Value;
import cn.taketoday.util.PropertyPlaceholderHandler;
import cn.taketoday.util.StringUtils;

import java.lang.reflect.Field;

/**
 * Resolve {@link Value} and {@link Env} annotation property.
 *
 * @author TODAY 2018-08-04 15:58
 * @see Env
 * @see Value
 * @see Required
 */
public class ValuePropertyResolver implements PropertyValueResolver {

  /**
   * Resolve {@link Value} and {@link Env} annotation property.
   */
  @Nullable
  @Override
  public PropertySetter resolveProperty(
          PropertyResolvingContext context, Field field) {
    AnnotationAttributes attributes = AnnotationUtils.getAttributes(Value.class, field);
    if (attributes != null) {
      ExpressionInfo expressionInfo = new ExpressionInfo(attributes, false);
      return resolve(context, field, expressionInfo);
    }
    else if ((attributes = AnnotationUtils.getAttributes(Env.class, field)) != null) {
      ExpressionInfo expressionInfo = new ExpressionInfo(attributes, true);
      return resolve(context, field, expressionInfo);
    }
    else {
      return null;
    }
  }

  private PropertySetter resolve(PropertyResolvingContext context, Field field, ExpressionInfo expr) {
    ExpressionEvaluator evaluator = context.getExpressionEvaluator();

    String expression = expr.getExpression();
    if (StringUtils.isEmpty(expression)) {
      // use class full name and field name
      expression = PropertyPlaceholderHandler.PLACEHOLDER_PREFIX +
              field.getDeclaringClass().getName() +
              Constant.PACKAGE_SEPARATOR +
              field.getName() +
              PropertyPlaceholderHandler.PLACEHOLDER_SUFFIX;
      expr.setPlaceholderOnly(false);
      expr.setExpression(expression);
    }

    Object value = evaluator.evaluate(expr, field.getType());
    if (value == null && AnnotationUtils.isPresent(field, Required.class)) {// perform @Required Annotation
      throw new ConfigurationException(
              "Can't resolve expression of field: [" + field +
                      "] with expression: [" + expr.getExpression() + "].");
    }
    return new DefaultPropertySetter(value, field);
  }

}
