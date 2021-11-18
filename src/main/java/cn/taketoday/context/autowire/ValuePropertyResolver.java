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
package cn.taketoday.context.autowire;

import cn.taketoday.beans.dependency.DefaultDependencySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.context.expression.ExpressionEvaluator;
import cn.taketoday.context.expression.ExpressionInfo;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Required;
import cn.taketoday.lang.Value;
import cn.taketoday.util.PropertyPlaceholderHandler;
import cn.taketoday.util.StringUtils;

/**
 * Resolve {@link Value} and {@link Env} annotation property.
 *
 * @author TODAY 2018-08-04 15:58
 * @see Env
 * @see Value
 * @see Required
 */
@Deprecated
public class ValuePropertyResolver implements PropertyValueResolver {

  /**
   * Resolve {@link Value} and {@link Env} annotation property.
   */
  @Nullable
  @Override
  public DefaultDependencySetter resolveProperty(
          PropertyResolvingContext context, BeanProperty property) {
    MergedAnnotations annotations = MergedAnnotations.from(property);
    MergedAnnotation<Value> annotation = annotations.get(Value.class);
    if (annotation.isPresent()) {
      ExpressionInfo expressionInfo = new ExpressionInfo(annotation, false);
      return resolve(context, property, expressionInfo);
    }
    MergedAnnotation<Env> env = annotations.get(Env.class);
    if (env.isPresent()) {
      ExpressionInfo expressionInfo = new ExpressionInfo(env, true);
      return resolve(context, property, expressionInfo);
    }
    return null;
  }

  private DefaultDependencySetter resolve(PropertyResolvingContext context, BeanProperty property, ExpressionInfo expr) {
    ExpressionEvaluator evaluator = context.getExpressionEvaluator();

    String expression = expr.getExpression();
    if (StringUtils.isEmpty(expression)) {
      // use class full name and field name
      expression = PropertyPlaceholderHandler.PLACEHOLDER_PREFIX +
              property.getDeclaringClass().getName() +
              Constant.PACKAGE_SEPARATOR +
              property.getName() +
              PropertyPlaceholderHandler.PLACEHOLDER_SUFFIX;
      expr.setPlaceholderOnly(false);
      expr.setExpression(expression);
    }

    Object value = evaluator.evaluate(expr, property.getType());
    if (value == null && AnnotatedElementUtils.isAnnotated(property, Required.class)) {
      // perform @Required Annotation
      throw new ConfigurationException(
              "Can't resolve expression of field: [" + property +
                      "] with expression: [" + expr.getExpression() + "].");
    }
    return new DefaultDependencySetter(value, property);
  }

}
