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

package infra.expression.spel.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;

import infra.expression.AccessException;
import infra.expression.ConstructorExecutor;
import infra.expression.EvaluationContext;
import infra.expression.TypedValue;
import infra.util.ReflectionUtils;

/**
 * A simple ConstructorExecutor implementation that runs a constructor using reflective
 * invocation.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ReflectiveConstructorExecutor implements ConstructorExecutor {

  private final Constructor<?> ctor;

  @Nullable
  private final Integer varargsPosition;

  public ReflectiveConstructorExecutor(Constructor<?> ctor) {
    this.ctor = ctor;
    if (ctor.isVarArgs()) {
      this.varargsPosition = ctor.getParameterCount() - 1;
    }
    else {
      this.varargsPosition = null;
    }
  }

  @Override
  @SuppressWarnings("NullAway")
  public TypedValue execute(EvaluationContext context, @Nullable Object... arguments) throws AccessException {
    try {
      ReflectionHelper.convertArguments(
              context.getTypeConverter(), arguments, this.ctor, this.varargsPosition);
      if (this.ctor.isVarArgs()) {
        arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(
                this.ctor.getParameterTypes(), arguments);
      }
      ReflectionUtils.makeAccessible(this.ctor);
      return new TypedValue(this.ctor.newInstance(arguments));
    }
    catch (Exception ex) {
      throw new AccessException("Problem invoking constructor: " + this.ctor, ex);
    }
  }

  public Constructor<?> getConstructor() {
    return this.ctor;
  }

}
