/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.beans.BeanUtils;
import infra.beans.TypeConverter;
import infra.core.ResolvableType;
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
