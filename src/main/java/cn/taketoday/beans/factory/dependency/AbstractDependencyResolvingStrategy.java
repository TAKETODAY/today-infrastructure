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

package cn.taketoday.beans.factory.dependency;

import java.lang.reflect.Parameter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 23:04</a>
 * @since 4.0
 */
public abstract class AbstractDependencyResolvingStrategy implements DependencyResolvingStrategy {

  @Override
  public final void resolveDependency(DependencyDescriptor injectionPoint, DependencyResolvingContext context) {
    if (supportsDependency(injectionPoint, context)) {
      Object internal = resolveInternal(injectionPoint, context);
      context.setDependency(internal);
    }
  }

  /**
   * If this {@link DependencyResolvingStrategy} supports target {@link InjectionPoint}
   *
   * @param injectionPoint Target method {@link Parameter}
   * @param context resolving context
   * @return If supports target {@link Parameter}
   */
  protected abstract boolean supportsDependency(
          InjectionPoint injectionPoint, DependencyResolvingContext context);

  protected abstract Object resolveInternal(
          InjectionPoint injectionPoint, DependencyResolvingContext context);

}
