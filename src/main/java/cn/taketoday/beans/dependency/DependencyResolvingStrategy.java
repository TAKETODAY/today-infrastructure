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

import cn.taketoday.beans.DependencyResolvingFailedException;

import java.lang.reflect.Parameter;

/**
 * resolve dependency
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 22:36</a>
 * @since 4.0
 */
public interface DependencyResolvingStrategy {

  /**
   * Resolve method/constructor parameter object
   * <p>
   * <b>NOTE<b/>: user must consider {@code resolvingContext}'s bean-factory is null or not
   * </p>
   *
   * @param injectionPoint Target method {@link Parameter} or a {@link java.lang.reflect.Field}
   * @param context resolving context never {@code null}
   * @throws DependencyResolvingFailedException dependency cannot determine or resolve
   */
  void resolveDependency(DependencyInjectionPoint injectionPoint, DependencyResolvingContext context);

}
