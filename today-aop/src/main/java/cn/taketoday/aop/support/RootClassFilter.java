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

package cn.taketoday.aop.support;

import java.io.Serializable;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.lang.Assert;

/**
 * Simple ClassFilter implementation that passes classes (and optionally subclasses).
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @author TODAY 2021/2/3 23:45
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RootClassFilter implements ClassFilter, Serializable {

  private final Class<?> clazz;

  public RootClassFilter(Class<?> clazz) {
    Assert.notNull(clazz, "Class is required");
    this.clazz = clazz;
  }

  @Override
  public boolean matches(Class<?> candidate) {
    return this.clazz.isAssignableFrom(candidate);
  }

  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof RootClassFilter &&
            this.clazz.equals(((RootClassFilter) other).clazz)));
  }

  @Override
  public int hashCode() {
    return this.clazz.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + this.clazz.getName();
  }

}
