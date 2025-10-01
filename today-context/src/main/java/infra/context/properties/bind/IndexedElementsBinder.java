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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.ConfigurationPropertyName.Form;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.IterableConfigurationPropertySource;
import infra.core.ResolvableType;

/**
 * Base class for {@link AggregateBinder AggregateBinders} that read a sequential run of
 * indexed items.
 *
 * @param <T> the type being bound
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class IndexedElementsBinder<T> extends AggregateBinder<T> {

  private static final String[] INDEXES;

  static {
    INDEXES = new String[10];
    for (int i = 0; i < INDEXES.length; i++) {
      INDEXES[i] = "[" + i + "]";
    }
  }

  IndexedElementsBinder(Binder.Context context) {
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
  protected final void bindIndexed(ConfigurationPropertyName name, Bindable<?> target, AggregateElementBinder elementBinder,
          ResolvableType aggregateType, ResolvableType elementType, IndexedCollectionSupplier result) {
    for (ConfigurationPropertySource source : context.getSources()) {
      bindIndexed(source, name, target, elementBinder, result, aggregateType, elementType);
      if (result.wasSupplied() && result.get() != null) {
        return;
      }
    }
  }

  private void bindIndexed(ConfigurationPropertySource source, ConfigurationPropertyName root, Bindable<?> target,
          AggregateElementBinder elementBinder, IndexedCollectionSupplier collection, ResolvableType aggregateType,
          ResolvableType elementType) {
    ConfigurationProperty property = source.getConfigurationProperty(root);
    if (property != null) {
      context.setConfigurationProperty(property);
      bindValue(target, collection.get(), aggregateType, elementType, property.getValue());
    }
    else {
      bindIndexed(source, root, elementBinder, collection, elementType);
    }
  }

  private void bindValue(Bindable<?> target, Collection<Object> collection, ResolvableType aggregateType,
          ResolvableType elementType, @Nullable Object value) {
    if (value == null || (value instanceof CharSequence cs && cs.isEmpty())) {
      return;
    }
    Object aggregate = convert(value, aggregateType, target.getAnnotations());
    ResolvableType collectionType = ResolvableType.forClassWithGenerics(collection.getClass(), elementType);
    Collection<Object> elements = convert(aggregate, collectionType);
    collection.addAll(elements);
  }

  private void bindIndexed(ConfigurationPropertySource source, ConfigurationPropertyName root,
          AggregateElementBinder elementBinder, IndexedCollectionSupplier collection, ResolvableType elementType) {
    Set<String> knownIndexedChildren = Collections.emptySet();
    if (source instanceof IterableConfigurationPropertySource iterableSource) {
      knownIndexedChildren = getKnownIndexedChildren(iterableSource, root);
    }
    for (int i = 0; i < Integer.MAX_VALUE; i++) {
      ConfigurationPropertyName name = appendIndex(root, i);
      Object value = elementBinder.bind(name, Bindable.of(elementType), source);
      if (value == null) {
        break;
      }
      if (!knownIndexedChildren.isEmpty()) {
        knownIndexedChildren.remove(name.getLastElement(Form.UNIFORM));
      }
      collection.get().add(value);
    }
    if (source instanceof IterableConfigurationPropertySource iterableSource) {
      assertNoUnboundChildren(knownIndexedChildren, iterableSource, root);
    }
  }

  private Set<String> getKnownIndexedChildren(IterableConfigurationPropertySource source, ConfigurationPropertyName root) {
    var knownIndexedChildren = new HashSet<String>();
    for (ConfigurationPropertyName name : source.filter(root::isAncestorOf)) {
      ConfigurationPropertyName choppedName = name.chop(root.getNumberOfElements() + 1);
      if (choppedName.isLastElementIndexed()) {
        knownIndexedChildren.add(choppedName.getLastElement(Form.UNIFORM));
      }
    }
    return knownIndexedChildren;
  }

  private void assertNoUnboundChildren(Set<String> unboundIndexedChildren,
          IterableConfigurationPropertySource filteredSource, ConfigurationPropertyName root) {
    if (unboundIndexedChildren.isEmpty()) {
      return;
    }
    TreeSet<ConfigurationProperty> unboundProperties = new TreeSet<>();
    for (ConfigurationPropertyName name : filteredSource) {
      ConfigurationPropertyName choppedName = name.chop(root.getNumberOfElements() + 1);
      if (choppedName.isLastElementIndexed()
              && unboundIndexedChildren.contains(choppedName.getLastElement(Form.UNIFORM))) {
        unboundProperties.add(filteredSource.getConfigurationProperty(name));
      }
    }
    if (!unboundProperties.isEmpty()) {
      throw new UnboundConfigurationPropertiesException(unboundProperties);
    }
  }

  private ConfigurationPropertyName appendIndex(ConfigurationPropertyName root, int i) {
    return root.append((i < INDEXES.length) ? INDEXES[i] : "[" + i + "]");
  }

  @Nullable
  private <C> C convert(@Nullable Object value, ResolvableType type, Annotation... annotations) {
    value = context.getPlaceholdersResolver().resolvePlaceholders(value);
    return context.getConverter().convert(value, type, annotations);
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
