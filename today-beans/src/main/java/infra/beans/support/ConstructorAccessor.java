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

package infra.beans.support;

import java.lang.reflect.Constructor;

import infra.lang.Nullable;
import infra.reflect.Accessor;

/**
 * @author TODAY 2021/8/27 21:41
 */
public abstract class ConstructorAccessor extends BeanInstantiator implements Accessor {

  protected final Constructor<?> constructor;

  public ConstructorAccessor(Constructor<?> constructor) {
    this.constructor = constructor;
  }

  @Override
  public Constructor<?> getConstructor() {
    return constructor;
  }

  /**
   * Invoke {@link java.lang.reflect.Constructor} with given args
   *
   * @return returns Object
   * @since 4.0
   */
  @Override
  public abstract Object doInstantiate(@Nullable Object[] args);
}
