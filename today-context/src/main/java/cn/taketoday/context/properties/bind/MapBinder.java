/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import cn.taketoday.context.properties.bind.Binder.Context;
import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertyName.Form;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.ConfigurationPropertyState;
import cn.taketoday.context.properties.source.IterableConfigurationPropertySource;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * {@link AggregateBinder} for Maps.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class MapBinder extends AggregateBinder<Map<Object, Object>> {

  private static final Bindable<Map<String, String>>
          STRING_STRING_MAP = Bindable.mapOf(String.class, String.class);

  MapBinder(Context context) {
    super(context);
  }

  @Override
  protected boolean isAllowRecursiveBinding(ConfigurationPropertySource source) {
    return true;
  }

  @Override
  protected Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target, AggregateElementBinder elementBinder) {
    Bindable<?> resolvedTarget = resolveTarget(target);

    if (!hasDescendants(name) && !ConfigurationPropertyName.EMPTY.equals(name)) {
      for (ConfigurationPropertySource source : context.getSources()) {
        ConfigurationProperty property = source.getConfigurationProperty(name);
        if (property != null) {
          context.setConfigurationProperty(property);
          Object result = context.getPlaceholdersResolver().resolvePlaceholders(property.getValue());
          return context.getConverter().convert(result, target);
        }
      }
    }

    Map<Object, Object> map = createMap(target);
    for (ConfigurationPropertySource source : context.getSources()) {
      if (!ConfigurationPropertyName.EMPTY.equals(name)) {
        source = source.filter(name::isAncestorOf);
      }
      new EntryBinder(name, resolvedTarget, elementBinder).bindEntries(source, map);
    }
    return map.isEmpty() ? null : map;
  }

  private Map<Object, Object> createMap(Bindable<?> target) {
    Class<?> mapType = (target.getValue() != null) ? Map.class : target.getType().resolve(Object.class);
    if (EnumMap.class.isAssignableFrom(mapType)) {
      Class<?> keyType = target.getType().asMap().resolveGeneric(0);
      return CollectionUtils.createMap(mapType, keyType, 0);
    }
    return CollectionUtils.createMap(mapType, 0);
  }

  private boolean hasDescendants(ConfigurationPropertyName name) {
    for (ConfigurationPropertySource source : context.getSources()) {
      if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
        return true;
      }
    }
    return false;
  }

  private Bindable<?> resolveTarget(Bindable<?> target) {
    Class<?> type = target.getType().resolve(Object.class);
    if (Properties.class.isAssignableFrom(type)) {
      return STRING_STRING_MAP;
    }
    return target;
  }

  @Override
  protected Map<Object, Object> merge(Supplier<Map<Object, Object>> existing, Map<Object, Object> additional) {
    Map<Object, Object> existingMap = getExistingIfPossible(existing);
    if (existingMap == null) {
      return additional;
    }
    try {
      existingMap.putAll(additional);
      return copyIfPossible(existingMap);
    }
    catch (UnsupportedOperationException ex) {
      Map<Object, Object> result = createNewMap(additional.getClass(), existingMap);
      result.putAll(additional);
      return result;
    }
  }

  @Nullable
  private Map<Object, Object> getExistingIfPossible(Supplier<Map<Object, Object>> existing) {
    try {
      return existing.get();
    }
    catch (Exception ex) {
      return null;
    }
  }

  private Map<Object, Object> copyIfPossible(Map<Object, Object> map) {
    try {
      return createNewMap(map.getClass(), map);
    }
    catch (Exception ex) {
      return map;
    }
  }

  private Map<Object, Object> createNewMap(Class<?> mapClass, Map<Object, Object> map) {
    Map<Object, Object> result = CollectionUtils.createMap(mapClass, map.size());
    result.putAll(map);
    return result;
  }

  private class EntryBinder {

    private final ConfigurationPropertyName root;

    private final AggregateElementBinder elementBinder;

    private final ResolvableType mapType;

    private final ResolvableType keyType;

    private final ResolvableType valueType;

    EntryBinder(ConfigurationPropertyName root, Bindable<?> target, AggregateElementBinder elementBinder) {
      this.root = root;
      this.elementBinder = elementBinder;
      this.mapType = target.getType().asMap();
      this.keyType = this.mapType.getGeneric(0);
      this.valueType = this.mapType.getGeneric(1);
    }

    void bindEntries(ConfigurationPropertySource source, Map<Object, Object> map) {
      if (source instanceof IterableConfigurationPropertySource names) {
        BindConverter converter = context.getConverter();
        for (ConfigurationPropertyName name : names) {
          Bindable<?> valueBindable = getValueBindable(name);
          ConfigurationPropertyName entryName = getEntryName(source, name);
          Object key = converter.convert(getKeyName(entryName), this.keyType);
          map.computeIfAbsent(key, (k) -> this.elementBinder.bind(entryName, valueBindable));
        }
      }
    }

    private Bindable<?> getValueBindable(ConfigurationPropertyName name) {
      if (!this.root.isParentOf(name) && isValueTreatedAsNestedMap()) {
        return Bindable.of(this.mapType);
      }
      return Bindable.of(this.valueType);
    }

    private ConfigurationPropertyName getEntryName(
            ConfigurationPropertySource source, ConfigurationPropertyName name) {
      Class<?> resolved = this.valueType.resolve(Object.class);
      if (Collection.class.isAssignableFrom(resolved) || this.valueType.isArray()) {
        return chopNameAtNumericIndex(name);
      }
      if (!this.root.isParentOf(name) && (isValueTreatedAsNestedMap() || !isScalarValue(source, name))) {
        return name.chop(this.root.getNumberOfElements() + 1);
      }
      return name;
    }

    private ConfigurationPropertyName chopNameAtNumericIndex(ConfigurationPropertyName name) {
      int start = this.root.getNumberOfElements() + 1;
      int size = name.getNumberOfElements();
      for (int i = start; i < size; i++) {
        if (name.isNumericIndex(i)) {
          return name.chop(i);
        }
      }
      return name;
    }

    private boolean isValueTreatedAsNestedMap() {
      return Object.class.equals(this.valueType.resolve(Object.class));
    }

    private boolean isScalarValue(ConfigurationPropertySource source, ConfigurationPropertyName name) {
      Class<?> resolved = this.valueType.resolve(Object.class);
      if (!resolved.getName().startsWith("java.lang") && !resolved.isEnum()) {
        return false;
      }
      ConfigurationProperty property = source.getConfigurationProperty(name);
      if (property == null) {
        return false;
      }
      Object value = property.getValue();
      value = context.getPlaceholdersResolver().resolvePlaceholders(value);
      return context.getConverter().canConvert(value, this.valueType);
    }

    private String getKeyName(ConfigurationPropertyName name) {
      StringBuilder result = new StringBuilder();
      for (int i = this.root.getNumberOfElements(); i < name.getNumberOfElements(); i++) {
        if (result.length() != 0) {
          result.append('.');
        }
        result.append(name.getElement(i, Form.ORIGINAL));
      }
      return result.toString();
    }

  }

}
