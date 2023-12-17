/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.bind;

import cn.taketoday.context.properties.bind.Binder.Context;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.lang.Nullable;

/**
 * Internal strategy used by {@link Binder} to bind data objects. A data object is an
 * object composed itself of recursively bound properties.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JavaBeanBinder
 * @see ValueObjectBinder
 * @since 4.0
 */
interface DataObjectBinder {

  /**
   * Return a bound instance or {@code null} if the {@link DataObjectBinder} does not
   * support the specified {@link Bindable}.
   *
   * @param <T> the source type
   * @param name the name being bound
   * @param target the bindable to bind
   * @param context the bind context
   * @param propertyBinder property binder
   * @return a bound instance or {@code null}
   */
  @Nullable
  <T> T bind(ConfigurationPropertyName name, Bindable<T> target,
          Context context, DataObjectPropertyBinder propertyBinder);

  /**
   * Return a newly created instance or {@code null} if the {@link DataObjectBinder}
   * does not support the specified {@link Bindable}.
   *
   * @param <T> the source type
   * @param target the bindable to create
   * @param context the bind context
   * @return the created instance
   */
  @Nullable
  <T> T create(Bindable<T> target, Context context);

  /**
   * Callback that can be used to add additional suppressed exceptions when an instance
   * cannot be created.
   *
   * @param <T> the source type
   * @param target the bindable that was being created
   * @param context the bind context
   * @param exception the exception about to be thrown
   */
  default <T> void onUnableToCreateInstance(Bindable<T> target, Binder.Context context, RuntimeException exception) {
    // noop
  }

}
