/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.expression.spel.support;

import java.lang.reflect.Constructor;

import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.ConstructorExecutor;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

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
  public TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException {
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
