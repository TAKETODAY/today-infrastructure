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

import java.util.function.Supplier;

import infra.reflect.MethodAccessor;
import infra.util.function.SingletonSupplier;

/**
 * @author TODAY 2020/9/20 20:41
 */
final class MethodAccessorBeanInstantiator
        extends StaticMethodAccessorBeanInstantiator {

  private final Supplier<Object> obj;

  MethodAccessorBeanInstantiator(MethodAccessor accessor, Object obj) {
    this(accessor, SingletonSupplier.valueOf(obj));
  }

  MethodAccessorBeanInstantiator(MethodAccessor accessor, Supplier<Object> obj) {
    super(accessor);
    this.obj = obj;
  }

  @Override
  protected Object getObject() {
    return obj.get();
  }

  @Override
  public String toString() {
    return "BeanInstantiator for instance method: " + accessor.getMethod();
  }
}
