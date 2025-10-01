/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * Binder that can be used by {@link DataObjectBinder} implementations to bind the data
 * object properties.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Madhura Bhave
 * @since 4.0
 */
interface DataObjectPropertyBinder {

  /**
   * Bind the given property.
   *
   * @param propertyName the property name (in lowercase dashed form, e.g.
   * {@code first-name})
   * @param target the target bindable
   * @return the bound value or {@code null}
   */
  @Nullable
  Object bindProperty(String propertyName, Bindable<?> target);

}
