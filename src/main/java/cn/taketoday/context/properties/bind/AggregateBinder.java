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

import cn.taketoday.boot.context.properties.bind.Binder.Context;
import cn.taketoday.boot.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.boot.context.properties.source.ConfigurationPropertySource;

import java.util.function.Supplier;

/**
 * Internal strategy used by {@link Binder} to bind aggregates (Maps, Lists, Arrays).
 *
 * @param <T> the type being bound
 * @author Phillip Webb
 * @author Madhura Bhave
 */
abstract class AggregateBinder<T> {

	private final Context context;

	AggregateBinder(Context context) {
		this.context = context;
	}

	/**
	 * Determine if recursive binding is supported.
	 * @param source the configuration property source or {@code null} for all sources.
	 * @return if recursive binding is supported
	 */
	protected abstract boolean isAllowRecursiveBinding(ConfigurationPropertySource source);

	/**
	 * Perform binding for the aggregate.
	 * @param name the configuration property name to bind
	 * @param target the target to bind
	 * @param elementBinder an element binder
	 * @return the bound aggregate or null
	 */
	@SuppressWarnings("unchecked")
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
	 * @param name the configuration property name to bind
	 * @param target the target to bind
	 * @param elementBinder an element binder
	 * @return the bound result
	 */
	protected abstract Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder);

	/**
	 * Merge any additional elements into the existing aggregate.
	 * @param existing the supplier for the existing value
	 * @param additional the additional elements to merge
	 * @return the merged result
	 */
	protected abstract T merge(Supplier<T> existing, T additional);

	/**
	 * Return the context being used by this binder.
	 * @return the context
	 */
	protected final Context getContext() {
		return this.context;
	}

	/**
	 * Internal class used to supply the aggregate and cache the value.
	 *
	 * @param <T> the aggregate type
	 */
	protected static class AggregateSupplier<T> {

		private final Supplier<T> supplier;

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
