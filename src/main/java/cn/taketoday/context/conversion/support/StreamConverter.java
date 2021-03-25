/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.context.conversion.support;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.context.GenericDescriptor;
import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.utils.CollectionUtils;

/**
 * Converts a {@link Stream} to and from a collection or array,
 * converting the element type if necessary.
 *
 * @author Stephane Nicoll
 * @author TODAY
 * @since 3.o
 */
class StreamConverter implements TypeConverter {
  private final ConversionService conversionService;

  public StreamConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(final GenericDescriptor targetType, final Class<?> sourceType) {
    // Stream.class, Collection.class
    // Stream.class, Object[].class
    // Collection.class, Stream.class
    // Object[].class, Stream.class

    if (sourceType == Stream.class) {
      return targetType.isCollection() || targetType.isArray();
    }
    if (targetType.is(Stream.class)) {
      return CollectionUtils.isCollection(sourceType) || sourceType.isArray();
    }
    return false;
  }

  @Override
  public Object convert(final GenericDescriptor targetType, final Object source) {
    if (source instanceof Stream) {
      return convertFromStream((Stream<?>) source, targetType);
    }
    final GenericDescriptor elementDescriptor = targetType.getElementDescriptor();
    if (elementDescriptor == null) {
      // convert to Stream
      if (source instanceof Collection) {
        return ((Collection<?>) source).stream();
      }
      return Arrays.stream((Object[]) source);
    }
    else {
      if (source instanceof Collection) {
        final Collection<?> collection = (Collection<?>) source;
        final ArrayList target = new ArrayList<>(collection.size());

        for (final Object element : collection) {
          final Object converted = conversionService.convert(element, elementDescriptor);
          target.add(converted);
        }
        return target.stream();
      }
      else {
        // array
        final Object[] sourceArray = (Object[]) source;
        final ArrayList target = new ArrayList<>(sourceArray.length);
        for (final Object element : sourceArray) {
          final Object converted = conversionService.convert(element, elementDescriptor);
          target.add(converted);
        }
        return target.stream();
      }
    }
  }

  protected Object convertFromStream(Stream<?> source, GenericDescriptor targetType) {
    final class MapFunction implements UnaryOperator<Object> {
      final GenericDescriptor elementType;

      MapFunction(GenericDescriptor elementType) {
        this.elementType = elementType;
      }

      @Override
      public Object apply(final Object original) {
        return conversionService.convert(original, elementType);
      }
    }

    if (targetType.isCollection()) {
      final GenericDescriptor elementType = targetType.getGeneric(Collection.class);
      return source.map(new MapFunction(elementType))
              .collect(Collectors.toCollection(() -> CollectionUtils.createCollection(
                      targetType.getType(),  elementType != null ? elementType.getType() : null, 16)));
    }
    final GenericDescriptor elementType = targetType.getElementDescriptor();
    return source.map(new MapFunction(elementType))
            .toArray(count -> (Object[]) Array.newInstance(elementType.getType(), count));
  }

}
