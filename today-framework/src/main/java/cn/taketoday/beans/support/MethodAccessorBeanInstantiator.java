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
package cn.taketoday.beans.support;

import java.util.function.Supplier;

import cn.taketoday.core.reflect.MethodAccessor;
import cn.taketoday.util.SingletonSupplier;

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
