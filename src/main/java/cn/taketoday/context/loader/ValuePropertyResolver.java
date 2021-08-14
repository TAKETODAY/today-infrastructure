/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.loader;

import java.lang.reflect.Field;

import cn.taketoday.beans.factory.DefaultPropertySetter;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Env;
import cn.taketoday.context.ExpressionEvaluator;
import cn.taketoday.context.Value;
import cn.taketoday.context.aware.OrderedApplicationContextSupport;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.Required;
import cn.taketoday.core.utils.AnnotationUtils;
import cn.taketoday.core.utils.StringUtils;


/**
 * @author TODAY <br>
 * 2018-08-04 15:58
 */
public class ValuePropertyResolver
        extends OrderedApplicationContextSupport implements PropertyValueResolver {

  private ExpressionEvaluator expressionEvaluator;

  public ValuePropertyResolver(ApplicationContext context) {
    this(context, Ordered.HIGHEST_PRECEDENCE - 1);
  }

  public ValuePropertyResolver(ApplicationContext context, int order) {
    super(order);
    setApplicationContext(context);
  }

  @Override
  public boolean supportsProperty(final Field field) {
    return AnnotationUtils.isPresent(field, Value.class)
            || AnnotationUtils.isPresent(field, Env.class);
  }

  @Override
  protected void initApplicationContext(ApplicationContext context) {
    super.initApplicationContext(context);
    this.expressionEvaluator = new ExpressionEvaluator(context);
  }

  /**
   * Resolve {@link Value} and {@link Env} annotation property.
   */
  @Override
  public DefaultPropertySetter resolveProperty(final Field field) {
    String expression;
    final Value value = AnnotationUtils.getAnnotation(Value.class, field);
    if (value != null) {
      expression = value.value();
    }
    else {
      final Env env = AnnotationUtils.getAnnotation(Env.class, field);
      expression = env.value();
      if (StringUtils.isNotEmpty(expression)) {
        expression = new StringBuilder(expression.length() + 3)//
                .append(Constant.PLACE_HOLDER_PREFIX)//
                .append(expression)//
                .append(Constant.PLACE_HOLDER_SUFFIX).toString();
      }
    }

    if (StringUtils.isEmpty(expression)) {
      // use class full name and field name
      expression = new StringBuilder(Constant.PLACE_HOLDER_PREFIX) //
              .append(field.getDeclaringClass().getName())//
              .append(Constant.PACKAGE_SEPARATOR)//
              .append(field.getName())//
              .append(Constant.PLACE_HOLDER_SUFFIX).toString();
    }
    Object resolved;
    try {
      resolved = expressionEvaluator.evaluate(expression, field.getType());
    }
    catch (ConfigurationException e) {
      return fallback(field, expression, e);
    }
    if (resolved == null) {
      return fallback(field, expression, null);
    }
    return new DefaultPropertySetter(resolved, field);
  }

  private DefaultPropertySetter fallback(final Field field,
                                         final String expression,
                                         ConfigurationException e) {
    boolean required;
    final Env env = AnnotationUtils.getAnnotation(Env.class, field);
    if (env == null) {
      final Value value = AnnotationUtils.getAnnotation(Value.class, field);
      required = value.required();
    }
    else {
      required = env.required();
    }
    if (required || AnnotationUtils.isPresent(field, Required.class)) {
      throw new ConfigurationException("Can't resolve field: [" + field + "] -> [" + expression + "].", e);
    }
    return null;
  }

}
