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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.bind;

import java.lang.reflect.Constructor;

import cn.taketoday.lang.Nullable;

/**
 * Strategy interface used to determine a specific constructor to use when binding.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface BindConstructorProvider {

  /**
   * Default {@link BindConstructorProvider} implementation that only returns a value
   * when there's a single constructor and when the bindable has no existing value.
   */
  BindConstructorProvider DEFAULT = new DefaultBindConstructorProvider();

  /**
   * Return the bind constructor to use for the given bindable, or {@code null} if
   * constructor binding is not supported.
   *
   * @param bindable the bindable to check
   * @param isNestedConstructorBinding if this binding is nested within a constructor
   * binding
   * @return the bind constructor or {@code null}
   */
  @Nullable
  Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding);

}
