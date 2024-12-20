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

import infra.core.env.Environment;
import infra.expression.AccessException;
import infra.expression.EvaluationContext;
import infra.expression.PropertyAccessor;
import infra.expression.TypedValue;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Read-only EL property accessor that knows how to retrieve keys
 * of a {@link Environment} instance.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class EnvironmentAccessor implements PropertyAccessor {

  @Override
  public Class<?>[] getSpecificTargetClasses() {
    return new Class<?>[] { Environment.class };
  }

  /**
   * Can read any {@link Environment}, thus always returns true.
   *
   * @return true
   */
  @Override
  public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    return true;
  }

  /**
   * Access the given target object by resolving the given property name against the given target
   * environment.
   */
  @Override
  public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    Assert.state(target instanceof Environment, "Target must be of type Environment");
    return new TypedValue(((Environment) target).getProperty(name));
  }

  /**
   * Read-only: returns {@code false}.
   */
  @Override
  public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    return false;
  }

  /**
   * Read-only: no-op.
   */
  @Override
  public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
          throws AccessException {
  }

}
