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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;

import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.lang.Assert;

/**
 * DependencyResolvingStrategy Decorator
 * <p>
 * Decorator Pattern
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/1 11:35
 */
public class DependencyResolvingDecorator implements DependencyResolvingStrategy {
  private final DependencyResolvingStrategy delegate;

  public DependencyResolvingDecorator(DependencyResolvingStrategy delegate) {
    Assert.notNull(delegate, "DependencyResolvingStrategy delegate is required");
    this.delegate = delegate;
  }

  @Override
  public boolean supports(Field field) {
    return delegate.supports(field);
  }

  @Override
  public boolean supports(Executable method) {
    return delegate.supports(method);
  }

  @Override
  public void resolveDependency(DependencyDescriptor descriptor, DependencyResolvingContext context) {
    delegate.resolveDependency(descriptor, context);
  }

}
