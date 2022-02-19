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
import cn.taketoday.core.ResolvableType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link AggregateBinder} for arrays.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ArrayBinder extends IndexedElementsBinder<Object> {

	ArrayBinder(Context context) {
		super(context);
	}

	@Override
	protected Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder) {
		IndexedCollectionSupplier result = new IndexedCollectionSupplier(ArrayList::new);
		ResolvableType aggregateType = target.getType();
		ResolvableType elementType = target.getType().getComponentType();
		bindIndexed(name, target, elementBinder, aggregateType, elementType, result);
		if (result.wasSupplied()) {
			List<Object> list = (List<Object>) result.get();
			Object array = Array.newInstance(elementType.resolve(), list.size());
			for (int i = 0; i < list.size(); i++) {
				Array.set(array, i, list.get(i));
			}
			return array;
		}
		return null;
	}

	@Override
	protected Object merge(Supplier<Object> existing, Object additional) {
		return additional;
	}

}
