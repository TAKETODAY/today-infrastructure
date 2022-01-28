/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.conversion.support;

import java.util.ArrayList;
import java.util.Map;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.MatchingConverter;
import cn.taketoday.util.CollectionUtils;

/**
 * Converts a Map to another Map.
 *
 * <p>First, creates a new Map of the requested targetType with a size equal to the
 * size of the source Map. Then copies each element in the source map to the target map.
 * Will perform a conversion from the source maps's parameterized K,V types to the target
 * map's parameterized types K,V if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class MapToMapConverter implements MatchingConverter {

  private final ConversionService conversionService;

  public MapToMapConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(final TypeDescriptor targetType, final Class<?> sourceType) {
    // Map.class, Map.class
    return targetType.isAssignableTo(Map.class)
            && Map.class.isAssignableFrom(sourceType);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object convert(final TypeDescriptor targetType, final Object source) {
    final Map<Object, Object> sourceMap = (Map<Object, Object>) source;

    // Shortcut if possible...
    boolean copyRequired = !targetType.isInstance(source);
    if (!copyRequired && sourceMap.isEmpty()) {
      return sourceMap;
    }

    final TypeDescriptor targetKeyType = targetType.getMapKeyDescriptor();
    final TypeDescriptor targetValueType = targetType.getMapValueDescriptor();

    final ConversionService conversionService = this.conversionService;
    final ArrayList<MapEntry> targetEntries = new ArrayList<>(sourceMap.size());
    for (final Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
      final Object sourceKey = entry.getKey();
      final Object sourceValue = entry.getValue();

      final Object targetKey = convertKey(sourceKey, targetKeyType, conversionService);
      final Object targetValue = convertValue(sourceValue, targetValueType, conversionService);

      targetEntries.add(new MapEntry(targetKey, targetValue));
      if (sourceKey != targetKey || sourceValue != targetValue) {
        copyRequired = true;
      }
    }

    if (!copyRequired) {
      return sourceMap;
    }

    final Map<Object, Object> targetMap = CollectionUtils.createMap(
            targetType.getType(), targetKeyType != null ? targetKeyType.getType() : null, sourceMap.size());
    for (MapEntry entry : targetEntries) {
      entry.addToMap(targetMap);
    }
    return targetMap;
  }

  // internal helpers

  private static Object convertKey(Object sourceKey, TypeDescriptor targetType, ConversionService conversionService) {
    if (targetType == null) {
      return sourceKey;
    }
    return conversionService.convert(sourceKey, targetType);
  }

  private static Object convertValue(Object sourceValue, TypeDescriptor targetType, ConversionService conversionService) {
    if (targetType == null) {
      return sourceValue;
    }
    return conversionService.convert(sourceValue, targetType);
  }

  record MapEntry(Object key, Object value) {

    void addToMap(Map<Object, Object> map) {
      map.put(this.key, this.value);
    }
  }

}
