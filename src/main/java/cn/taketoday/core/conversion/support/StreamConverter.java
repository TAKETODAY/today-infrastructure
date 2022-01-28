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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.MatchingConverter;
import cn.taketoday.util.CollectionUtils;

/**
 * Converts a {@link Stream} to and from a collection or array,
 * converting the element type if necessary.
 *
 * @author Stephane Nicoll
 * @author TODAY
 * @since 3.o
 */
final class StreamConverter implements MatchingConverter {
  private final ConversionService conversionService;

  public StreamConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(TypeDescriptor targetType, Class<?> sourceType) {
    // Stream.class, Collection.class
    // Stream.class, Object[].class
    // Collection.class, Stream.class
    // Object[].class, Stream.class

    if (Stream.class.isAssignableFrom(sourceType)) {
      return targetType.isCollection() || targetType.isArray();
    }
    if (targetType.is(Stream.class)) {
      return CollectionUtils.isCollection(sourceType) || sourceType.isArray();
    }
    return false;
  }

  @Override
  public Object convert(TypeDescriptor targetType, Object source) {
    if (source instanceof Stream) {
      return convertFromStream((Stream<?>) source, targetType);
    }
    TypeDescriptor elementDescriptor = targetType.getElementDescriptor();
    if (elementDescriptor == null) {
      // convert to Stream
      if (source instanceof Collection) {
        return ((Collection<?>) source).stream();
      }
      else if (source instanceof Object[]) {
        return Arrays.stream((Object[]) source);
      }
    }
    else {
      if (source instanceof Collection<?> collection) {
        ArrayList<Object> target = new ArrayList<>(collection.size());

        for (Object element : collection) {
          Object converted = conversionService.convert(element, elementDescriptor);
          target.add(converted);
        }
        return target.stream();
      }
      else if (source instanceof Object[] sourceArray) {
        // array
        ArrayList<Object> target = new ArrayList<>(sourceArray.length);
        for (Object element : sourceArray) {
          Object converted = conversionService.convert(element, elementDescriptor);
          target.add(converted);
        }
        return target.stream();
      }
    }
    // Should not happen
    throw new IllegalStateException("Unexpected source/target types");
  }

  private Object convertFromStream(Stream<?> source, TypeDescriptor targetType) {
    final class MapFunction implements UnaryOperator<Object> {
      final TypeDescriptor elementType;

      MapFunction(TypeDescriptor elementType) {
        this.elementType = elementType;
      }

      @Override
      public Object apply(Object original) {
        return conversionService.convert(original, elementType);
      }
    }

    TypeDescriptor elementType = targetType.getElementDescriptor();
    if (elementType != null) {
      if (targetType.isCollection()) {
        return source.map(new MapFunction(elementType))
                .collect(Collectors.toCollection(() -> CollectionUtils.createCollection(
                        targetType.getType(), elementType.getType(), 16)));
      }
      return source.map(new MapFunction(elementType))
              .toArray(count -> (Object[]) Array.newInstance(elementType.getType(), count));
    }

    // elementType is null
    if (targetType.isCollection()) {
      return source.collect(Collectors.toCollection(() -> CollectionUtils.createCollection(
              targetType.getType(), null, 16)));
    }
    return source.toArray(Object[]::new);
  }

}
