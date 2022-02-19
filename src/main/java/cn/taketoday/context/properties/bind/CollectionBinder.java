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
import cn.taketoday.core.CollectionFactory;
import cn.taketoday.core.ResolvableType;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link AggregateBinder} for collections.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class CollectionBinder extends IndexedElementsBinder<Collection<Object>> {

	CollectionBinder(Context context) {
		super(context);
	}

	@Override
	protected Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder elementBinder) {
		Class<?> collectionType = (target.getValue() != null) ? List.class : target.getType().resolve(Object.class);
		ResolvableType aggregateType = ResolvableType.forClassWithGenerics(List.class,
				target.getType().asCollection().getGenerics());
		ResolvableType elementType = target.getType().asCollection().getGeneric();
		IndexedCollectionSupplier result = new IndexedCollectionSupplier(
				() -> CollectionFactory.createCollection(collectionType, elementType.resolve(), 0));
		bindIndexed(name, target, elementBinder, aggregateType, elementType, result);
		if (result.wasSupplied()) {
			return result.get();
		}
		return null;
	}

	@Override
	protected Collection<Object> merge(Supplier<Collection<Object>> existing, Collection<Object> additional) {
		Collection<Object> existingCollection = getExistingIfPossible(existing);
		if (existingCollection == null) {
			return additional;
		}
		try {
			existingCollection.clear();
			existingCollection.addAll(additional);
			return copyIfPossible(existingCollection);
		}
		catch (UnsupportedOperationException ex) {
			return createNewCollection(additional);
		}
	}

	private Collection<Object> getExistingIfPossible(Supplier<Collection<Object>> existing) {
		try {
			return existing.get();
		}
		catch (Exception ex) {
			return null;
		}
	}

	private Collection<Object> copyIfPossible(Collection<Object> collection) {
		try {
			return createNewCollection(collection);
		}
		catch (Exception ex) {
			return collection;
		}
	}

	private Collection<Object> createNewCollection(Collection<Object> collection) {
		Collection<Object> result = CollectionFactory.createCollection(collection.getClass(), collection.size());
		result.addAll(collection);
		return result;
	}

}
