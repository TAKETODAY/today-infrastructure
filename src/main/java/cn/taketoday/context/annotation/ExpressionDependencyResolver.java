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

package cn.taketoday.context.annotation;

import java.lang.reflect.Field;

import cn.taketoday.beans.DependencyResolvingFailedException;
import cn.taketoday.beans.dependency.DependencyResolvingContext;
import cn.taketoday.beans.dependency.DependencyResolvingStrategy;
import cn.taketoday.beans.dependency.InjectionPoint;
import cn.taketoday.context.expression.ExpressionEvaluatorSupport;
import cn.taketoday.context.expression.ExpressionInfo;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.Value;
import cn.taketoday.util.PropertyPlaceholderHandler;
import cn.taketoday.util.StringUtils;

/**
 * for Env and Value
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/18 21:11</a>
 * @since 4.0
 */
public class ExpressionDependencyResolver
        extends ExpressionEvaluatorSupport implements DependencyResolvingStrategy, Ordered {

  @Override
  public void resolveDependency(InjectionPoint injectionPoint, DependencyResolvingContext context) {
    if (!context.hasDependency()) {
      MergedAnnotation<Env> env = injectionPoint.getAnnotation(Env.class);
      if (env.isPresent()) {
        ExpressionInfo expressionInfo = new ExpressionInfo(env, true);
        Object evaluate = resolve(injectionPoint, expressionInfo);
        context.setDependency(evaluate);
        context.terminate();
      }
      else {
        MergedAnnotation<Value> annotation = injectionPoint.getAnnotation(Value.class);
        if (annotation.isPresent()) {
          ExpressionInfo expressionInfo = new ExpressionInfo(annotation, false);
          Object evaluate = resolve(injectionPoint, expressionInfo);
          context.setDependency(evaluate);
          context.terminate();
        }
      }
    }
  }

  private Object resolve(InjectionPoint injectionPoint, ExpressionInfo expr) {
    String expression = expr.getExpression();
    if (StringUtils.isEmpty(expression)) {
      if (injectionPoint.isProperty()
              && injectionPoint.getTarget() instanceof Field property) {
        // use class full name and field name
        expression = PropertyPlaceholderHandler.PLACEHOLDER_PREFIX +
                property.getDeclaringClass().getName() +
                Constant.PACKAGE_SEPARATOR +
                property.getName() +
                PropertyPlaceholderHandler.PLACEHOLDER_SUFFIX;
        expr.setPlaceholderOnly(false);
        expr.setExpression(expression);
      }
    }

    Object value = evaluator().evaluate(expr, injectionPoint.getDependencyType());
    if (value == null && injectionPoint.isRequired()) {
      // perform @Required Annotation
      throw new DependencyResolvingFailedException(
              "Can't resolve expression on injection-point: [" + injectionPoint +
                      "] with expression: [" + expr.getExpression() + "].");
    }
    return value;
  }

  @Override
  public int getOrder() {
    return HIGHEST_PRECEDENCE;
  }

}
