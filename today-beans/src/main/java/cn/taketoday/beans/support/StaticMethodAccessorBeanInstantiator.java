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
package cn.taketoday.beans.support;

import cn.taketoday.reflect.MethodAccessor;

/**
 * @author TODAY 2020/9/20 20:35
 */
class StaticMethodAccessorBeanInstantiator extends BeanInstantiator {
  protected final MethodAccessor accessor;

  StaticMethodAccessorBeanInstantiator(final MethodAccessor accessor) {
    this.accessor = accessor;
  }

  @Override
  public final Object doInstantiate(final Object[] args) {
    return accessor.invoke(getObject(), args);
  }

  protected Object getObject() {
    return null;
  }

  @Override
  public String toString() {
    return "BeanInstantiator for static method: " + accessor.getMethod();
  }

}
