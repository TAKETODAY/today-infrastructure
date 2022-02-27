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

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanExpressionException;
import cn.taketoday.beans.factory.support.BeanExpressionContext;
import cn.taketoday.beans.factory.support.BeanExpressionResolver;
import cn.taketoday.expression.BeanNameExpressionResolver;
import cn.taketoday.expression.BeanNameResolver;
import cn.taketoday.expression.ExpressionFactory;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Standard implementation of the {@link BeanExpressionResolver} interface,
 * parsing and evaluating EL using {@code cn.taketoday.expression} module.
 *
 * <p>All beans in the containing {@code BeanFactory} are made available as
 * predefined variables with their common bean name, including standard context
 * beans such as "environment", "systemProperties" and "systemEnvironment".
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanExpressionContext#getBeanFactory()
 * @since 4.0 2021/12/25 15:01
 */
public class StandardBeanExpressionResolver implements BeanExpressionResolver {

  private final ExpressionFactory expressionFactory = ExpressionFactory.getSharedInstance();

  @Nullable
  @Override
  public Object evaluate(@Nullable String value, BeanExpressionContext evalContext) throws BeansException {
    if (StringUtils.isEmpty(value)) {
      return value;
    }
    if (!value.startsWith("#{") || !value.endsWith("}")) {
      return value;
    }
    try {
      StandardExpressionContext context = new StandardExpressionContext();
      context.addResolver(new BeanNameExpressionResolver(new BeanNameResolver() {
        @Override
        public boolean isNameResolved(String beanName) {
          return evalContext.containsObject(beanName);
        }

        @Override
        public Object getBean(String beanName) {
          return evalContext.getObject(beanName);
        }
      }));
      context.addResolver(new EnvironmentExpressionResolver());
      context.addResolver(new StandardTypeConverter());

      return expressionFactory.createValueExpression(context, value, Object.class).getValue(context);
    }
    catch (Throwable ex) {
      throw new BeanExpressionException("Expression '" + value + "' parsing failed", ex);
    }
  }

}
