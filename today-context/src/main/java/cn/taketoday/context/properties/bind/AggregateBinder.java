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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.bind;

import java.util.function.Supplier;

import cn.taketoday.context.properties.bind.Binder.Context;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.lang.Nullable;

/**
 * Internal strategy used by {@link Binder} to bind aggregates (Maps, Lists, Arrays).
 *
 * @param <T> the type being bound
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class AggregateBinder<T> {

  /**
   * The context being used by this binder.
   */
  public final Context context;

  AggregateBinder(Context context) {
    this.context = context;
  }

  /**
   * Determine if recursive binding is supported.
   *
   * @param source the configuration property source or {@code null} for all sources.
   * @return if recursive binding is supported
   */
  protected abstract boolean isAllowRecursiveBinding(@Nullable ConfigurationPropertySource source);

  /**
   * Perform binding for the aggregate.
   *
   * @param name the configuration property name to bind
   * @param target the target to bind
   * @param elementBinder an element binder
   * @return the bound aggregate or null
   */
  @SuppressWarnings("unchecked")
  @Nullable
  final Object bind(ConfigurationPropertyName name, Bindable<?> target, AggregateElementBinder elementBinder) {
    Object result = bindAggregate(name, target, elementBinder);
    Supplier<?> value = target.getValue();
    if (result == null || value == null) {
      return result;
    }
    return merge((Supplier<T>) value, (T) result);
  }

  /**
   * Perform the actual aggregate binding.
   *
   * @param name the configuration property name to bind
   * @param target the target to bind
   * @param elementBinder an element binder
   * @return the bound result
   */
  @Nullable
  protected abstract Object bindAggregate(
          ConfigurationPropertyName name, Bindable<?> target, AggregateElementBinder elementBinder);

  /**
   * Merge any additional elements into the existing aggregate.
   *
   * @param existing the supplier for the existing value
   * @param additional the additional elements to merge
   * @return the merged result
   */
  protected abstract T merge(Supplier<T> existing, T additional);

  /**
   * Internal class used to supply the aggregate and cache the value.
   *
   * @param <T> the aggregate type
   */
  protected static class AggregateSupplier<T> {

    private final Supplier<T> supplier;

    @Nullable
    private T supplied;

    public AggregateSupplier(Supplier<T> supplier) {
      this.supplier = supplier;
    }

    public T get() {
      if (this.supplied == null) {
        this.supplied = this.supplier.get();
      }
      return this.supplied;
    }

    public boolean wasSupplied() {
      return this.supplied != null;
    }

  }

}
