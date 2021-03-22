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
import java.util.Arrays;
import java.util.Collection;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.GenericTypeResolver;

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

  /**
   * @param targetType
   *         target class
   * @param source
   *         source object never be null
   */
  @Override
  public boolean supports(Class<?> targetType, Object source) {
    // Stream.class, Collection.class
    // Stream.class, Object[].class
    // Collection.class, Stream.class
    // Object[].class, Stream.class

    if (source instanceof Stream) {
      return CollectionUtils.isCollection(targetType) || targetType.isArray();
    }
    if (targetType == Stream.class) {
      return source instanceof Collection || source.getClass().isArray();
    }
    return false;
  }

  @Override
  public Object convert(Class<?> targetType, Object source) {
    if (source instanceof Stream) {
      return convertFromStream((Stream<?>) source, targetType);
    }
    // convert to Stream
    if (source instanceof Collection) {
      return ((Collection<?>) source).stream();
    }
    return Arrays.stream((Object[]) source);
  }

  protected Object convertFromStream(Stream<?> source, Class<?> targetType) {
    final class MapFunction implements UnaryOperator<Object> {
      final Class<?> elementType;

      MapFunction(Class<?> elementType) {
        this.elementType = elementType;
      }

      @Override
      public Object apply(Object original) {
        return conversionService.convert(original, elementType);
      }
    }

    if (CollectionUtils.isCollection(targetType)) {
      final Class<Object> elementType = GenericTypeResolver.resolveTypeArgument(targetType, Collection.class);
      return source.map(new MapFunction(elementType))
              .collect(Collectors.toCollection(() -> CollectionUtils.createCollection(targetType, elementType, 16)));
    }

    final Class<?> elementType = targetType.getComponentType();
    return source.map(new MapFunction(elementType))
            .toArray(count -> (Object[]) Array.newInstance(elementType, count));
  }

}
