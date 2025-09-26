/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.expression;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.BeanExpressionContext;
import infra.expression.AccessException;
import infra.expression.EvaluationContext;
import infra.expression.PropertyAccessor;
import infra.expression.TypedValue;
import infra.lang.Assert;

/**
 * EL property accessor that knows how to traverse the beans and contextual objects
 * of a {@link BeanExpressionContext}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 17:31
 */
public class BeanExpressionContextAccessor implements PropertyAccessor {

  @Override
  public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    return (target instanceof BeanExpressionContext && ((BeanExpressionContext) target).containsObject(name));
  }

  @Override
  public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    Assert.state(target instanceof BeanExpressionContext, "Target must be of type BeanExpressionContext");
    return new TypedValue(((BeanExpressionContext) target).getObject(name));
  }

  @Override
  public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    return false;
  }

  @Override
  public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
          throws AccessException {

    throw new AccessException("Beans in a BeanFactory are read-only");
  }

  @Override
  public Class<?>[] getSpecificTargetClasses() {
    return new Class<?>[] { BeanExpressionContext.class };
  }

}
