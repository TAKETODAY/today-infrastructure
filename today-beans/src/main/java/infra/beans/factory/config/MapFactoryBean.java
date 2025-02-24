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
package infra.beans.factory.config;

import java.util.Map;

import infra.beans.BeanUtils;
import infra.beans.TypeConverter;
import infra.core.ResolvableType;
import infra.lang.Nullable;
import infra.util.CollectionUtils;

/**
 * Simple factory for shared Map instances. Allows for central setup
 * of Maps via the "map" element in XML bean definitions.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SetFactoryBean
 * @see ListFactoryBean
 * @since 4.0 2021/11/30 13:57
 */
public class MapFactoryBean extends AbstractFactoryBean<Map<Object, Object>> {

  @Nullable
  private Map<?, ?> sourceMap;

  @SuppressWarnings("rawtypes")
  @Nullable
  private Class<? extends Map> targetMapClass;

  /**
   * Set the source Map, typically populated via XML "map" elements.
   */
  public void setSourceMap(Map<?, ?> sourceMap) {
    this.sourceMap = sourceMap;
  }

  /**
   * Set the class to use for the target Map. Can be populated with a fully
   * qualified class name when defined in a Framework application context.
   * <p>Default is a linked HashMap, keeping the registration order.
   *
   * @see java.util.LinkedHashMap
   */
  @SuppressWarnings("rawtypes")
  public void setTargetMapClass(@Nullable Class<? extends Map> targetMapClass) {
    if (targetMapClass == null) {
      throw new IllegalArgumentException("'targetMapClass' is required");
    }
    if (!Map.class.isAssignableFrom(targetMapClass)) {
      throw new IllegalArgumentException("'targetMapClass' must implement [java.util.Map]");
    }
    this.targetMapClass = targetMapClass;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Class<Map> getObjectType() {
    return Map.class;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Map<Object, Object> createBeanInstance() {
    if (this.sourceMap == null) {
      throw new IllegalArgumentException("'sourceMap' is required");
    }
    Map<Object, Object> result = null;
    if (this.targetMapClass != null) {
      result = BeanUtils.newInstance(this.targetMapClass);
    }
    else {
      result = CollectionUtils.newLinkedHashMap(this.sourceMap.size());
    }
    Class<?> keyType = null;
    Class<?> valueType = null;
    if (this.targetMapClass != null) {
      ResolvableType mapType = ResolvableType.forClass(this.targetMapClass).asMap();
      keyType = mapType.resolveGeneric(0);
      valueType = mapType.resolveGeneric(1);
    }
    if (keyType != null || valueType != null) {
      TypeConverter converter = getBeanTypeConverter();
      for (Map.Entry<?, ?> entry : this.sourceMap.entrySet()) {
        Object convertedKey = converter.convertIfNecessary(entry.getKey(), keyType);
        Object convertedValue = converter.convertIfNecessary(entry.getValue(), valueType);
        result.put(convertedKey, convertedValue);
      }
    }
    else {
      result.putAll(this.sourceMap);
    }
    return result;
  }

}
