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

package infra.context.properties.bind;

import java.lang.reflect.Constructor;

import infra.lang.Nullable;

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
   * Return the bind constructor to use for the given type, or {@code null} if
   * constructor binding is not supported.
   *
   * @param type the type to check
   * @param isNestedConstructorBinding if this binding is nested within a constructor
   * binding
   * @return the bind constructor or {@code null}
   */
  @Nullable
  default Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
    return getBindConstructor(Bindable.of(type), isNestedConstructorBinding);
  }

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
