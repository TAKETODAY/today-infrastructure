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

/**
 * Internal strategy used by {@link Binder} to bind data objects. A data object is an
 * object composed itself of recursively bound properties.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see JavaBeanBinder
 * @see ValueObjectBinder
 */
interface DataObjectBinder {

	/**
	 * Return a bound instance or {@code null} if the {@link DataObjectBinder} does not
	 * support the specified {@link Bindable}.
	 * @param name the name being bound
	 * @param target the bindable to bind
	 * @param context the bind context
	 * @param propertyBinder property binder
	 * @param <T> the source type
	 * @return a bound instance or {@code null}
	 */
	<T> T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
			DataObjectPropertyBinder propertyBinder);

	/**
	 * Return a newly created instance or {@code null} if the {@link DataObjectBinder}
	 * does not support the specified {@link Bindable}.
	 * @param target the bindable to create
	 * @param context the bind context
	 * @param <T> the source type
	 * @return the created instance
	 */
	<T> T create(Bindable<T> target, Context context);

}
