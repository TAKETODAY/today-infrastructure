/*
 * Copyright 2017 - 2024 the original author or authors.
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

import infra.beans.factory.BeanFactory;
import infra.expression.AccessException;
import infra.expression.EvaluationContext;
import infra.expression.PropertyAccessor;
import infra.expression.TypedValue;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * EL property accessor that knows how to traverse the beans of a
 * {@link BeanFactory}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0
 */
public class BeanFactoryAccessor implements PropertyAccessor {

  @Override
  public Class<?>[] getSpecificTargetClasses() {
    return new Class<?>[] { BeanFactory.class };
  }

  @Override
  public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    return (target instanceof BeanFactory && ((BeanFactory) target).containsBean(name));
  }

  @Override
  public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    Assert.state(target instanceof BeanFactory, "Target must be of type BeanFactory");
    return new TypedValue(((BeanFactory) target).getBean(name));
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

}
