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

import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.lang.Nullable;

/**
 * Binder that can be used by {@link AggregateBinder} implementations to recursively bind
 * elements.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
interface AggregateElementBinder {

  /**
   * Bind the given name to a target bindable.
   *
   * @param name the name to bind
   * @param target the target bindable
   * @return a bound object or {@code null}
   */
  @Nullable
  default Object bind(ConfigurationPropertyName name, Bindable<?> target) {
    return bind(name, target, null);
  }

  /**
   * Bind the given name to a target bindable using optionally limited to a single
   * source.
   *
   * @param name the name to bind
   * @param target the target bindable
   * @param source the source of the elements or {@code null} to use all sources
   * @return a bound object or {@code null}
   */
  @Nullable
  Object bind(ConfigurationPropertyName name,
              Bindable<?> target, @Nullable ConfigurationPropertySource source);

}
