/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.bind;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cn.taketoday.context.properties.bind.Binder.Context;
import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertyName.Form;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.IterableConfigurationPropertySource;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

/**
 * Base class for {@link AggregateBinder AggregateBinders} that read a sequential run of
 * indexed items.
 *
 * @param <T> the type being bound
 * @author Phillip Webb
 * @author Madhura Bhave
 */
abstract class IndexedElementsBinder<T> extends AggregateBinder<T> {

  private static final String INDEX_ZERO = "[0]";

  IndexedElementsBinder(Context context) {
    super(context);
  }

  @Override
  protected boolean isAllowRecursiveBinding(@Nullable ConfigurationPropertySource source) {
    return source == null || source instanceof IterableConfigurationPropertySource;
  }

  /**
   * Bind indexed elements to the supplied collection.
   *
   * @param name the name of the property to bind
   * @param target the target bindable
   * @param elementBinder the binder to use for elements
   * @param aggregateType the aggregate type, may be a collection or an array
   * @param elementType the element type
   * @param result the destination for results
   */
  protected final void bindIndexed(
          ConfigurationPropertyName name, Bindable<?> target,
          AggregateElementBinder elementBinder, ResolvableType aggregateType,
          ResolvableType elementType, IndexedCollectionSupplier result) {

    for (ConfigurationPropertySource source : getContext().getSources()) {
      bindIndexed(source, name, target, elementBinder, result, aggregateType, elementType);
      if (result.wasSupplied() && result.get() != null) {
        return;
      }
    }
  }

  private void bindIndexed(
          ConfigurationPropertySource source, ConfigurationPropertyName root, Bindable<?> target,
          AggregateElementBinder elementBinder, IndexedCollectionSupplier collection,
          ResolvableType aggregateType, ResolvableType elementType) {

    ConfigurationProperty property = source.getConfigurationProperty(root);
    if (property != null) {
      getContext().setConfigurationProperty(property);
      bindValue(target, collection.get(), aggregateType, elementType, property.getValue());
    }
    else {
      bindIndexed(source, root, elementBinder, collection, elementType);
    }
  }

  private void bindValue(
          Bindable<?> target, Collection<Object> collection,
          ResolvableType aggregateType, ResolvableType elementType, @Nullable Object value) {
    if (value == null || value instanceof CharSequence && ((CharSequence) value).length() == 0) {
      return;
    }
    Object aggregate = convert(value, aggregateType, target.getAnnotations());
    ResolvableType collectionType = ResolvableType.fromClassWithGenerics(collection.getClass(), elementType);
    Collection<Object> elements = convert(aggregate, collectionType);
    collection.addAll(elements);
  }

  private void bindIndexed(
          ConfigurationPropertySource source, ConfigurationPropertyName root,
          AggregateElementBinder elementBinder, IndexedCollectionSupplier collection, ResolvableType elementType) {

    MultiValueMap<String, ConfigurationPropertyName> knownIndexedChildren = getKnownIndexedChildren(source, root);
    for (int i = 0; i < Integer.MAX_VALUE; i++) {
      ConfigurationPropertyName name = root.append((i != 0) ? "[" + i + "]" : INDEX_ZERO);
      Object value = elementBinder.bind(name, Bindable.of(elementType), source);
      if (value == null) {
        break;
      }
      knownIndexedChildren.remove(name.getLastElement(Form.UNIFORM));
      collection.get().add(value);
    }
    assertNoUnboundChildren(source, knownIndexedChildren);
  }

  private MultiValueMap<String, ConfigurationPropertyName> getKnownIndexedChildren(
          ConfigurationPropertySource source, ConfigurationPropertyName root) {
    MultiValueMap<String, ConfigurationPropertyName> children = MultiValueMap.forLinkedHashMap();
    if (!(source instanceof IterableConfigurationPropertySource)) {
      return children;
    }
    for (ConfigurationPropertyName name : (IterableConfigurationPropertySource) source.filter(root::isAncestorOf)) {
      ConfigurationPropertyName choppedName = name.chop(root.getNumberOfElements() + 1);
      if (choppedName.isLastElementIndexed()) {
        String key = choppedName.getLastElement(Form.UNIFORM);
        children.add(key, name);
      }
    }
    return children;
  }

  private void assertNoUnboundChildren(
          ConfigurationPropertySource source,
          MultiValueMap<String, ConfigurationPropertyName> children) {
    if (!children.isEmpty()) {
      throw new UnboundConfigurationPropertiesException(
              children.values()
                      .stream()
                      .flatMap(List::stream)
                      .map(source::getConfigurationProperty)
                      .collect(Collectors.toCollection(TreeSet::new))
      );
    }
  }

  @Nullable
  private <C> C convert(Object value, ResolvableType type, Annotation... annotations) {
    value = getContext().getPlaceholdersResolver().resolvePlaceholders(value);
    return getContext().getConverter().convert(value, type, annotations);
  }

  /**
   * {@link AggregateBinder.AggregateSupplier AggregateSupplier} for an indexed
   * collection.
   */
  protected static class IndexedCollectionSupplier extends AggregateSupplier<Collection<Object>> {

    public IndexedCollectionSupplier(Supplier<Collection<Object>> supplier) {
      super(supplier);
    }

  }

}
