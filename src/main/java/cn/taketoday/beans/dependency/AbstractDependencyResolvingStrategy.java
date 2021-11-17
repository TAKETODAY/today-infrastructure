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

package cn.taketoday.beans.dependency;

import java.lang.reflect.Parameter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 23:04</a>
 * @since 4.0
 */
public abstract class AbstractDependencyResolvingStrategy implements DependencyResolvingStrategy {

  @Override
  public final void resolveDependency(DependencyInjectionPoint injectionPoint, DependencyResolvingContext resolvingContext) {
    if (supportsDependency(injectionPoint, resolvingContext)) {
      resolveInternal(injectionPoint, resolvingContext);
    }
  }

  /**
   * If this {@link DependencyResolvingStrategy} supports target {@link DependencyInjectionPoint}
   *
   * @param injectionPoint Target method {@link Parameter}
   * @param context resolving context
   * @return If supports target {@link Parameter}
   */
  protected abstract boolean supportsDependency(
          DependencyInjectionPoint injectionPoint, DependencyResolvingContext context);

  protected abstract Object resolveInternal(
          DependencyInjectionPoint injectionPoint, DependencyResolvingContext context);

}
