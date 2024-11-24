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

package infra.core.conversion;

import infra.core.TypeDescriptor;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * A service interface for type conversion. This is the entry point into the convert system.
 * Call {@link #convert(Object, Class)} to perform a thread-safe type conversion using this system.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/3/19 20:59
 */
public interface ConversionService {

  /**
   * Hook method to lookup the converter for a given sourceType/targetType pair.
   * First queries this ConversionService's converter cache.
   * On a cache miss, then performs an exhaustive search for a matching converter.
   * If no converter matches, returns the default converter.
   *
   * @param sourceType the source type to convert from
   * @param targetType the target type to convert to
   * @return the generic converter that will perform the conversion,
   * or {@code null} if no suitable converter was found
   */
  @Nullable
  default GenericConverter getConverter(Class<?> sourceType, TypeDescriptor targetType) {
    return getConverter(TypeDescriptor.valueOf(sourceType), targetType);
  }

  /**
   * Hook method to lookup the converter for a given sourceType/targetType pair.
   * First queries this ConversionService's converter cache.
   * On a cache miss, then performs an exhaustive search for a matching converter.
   * If no converter matches, returns the default converter.
   *
   * @param sourceObject the source to convert from
   * @param targetType the target type to convert to
   * @return the generic converter that will perform the conversion,
   * or {@code null} if no suitable converter was found
   */
  @Nullable
  default GenericConverter getConverter(Object sourceObject, TypeDescriptor targetType) {
    Assert.notNull(sourceObject, "source object is required");
    return getConverter(sourceObject.getClass(), targetType);
  }

  /**
   * Hook method to lookup the converter for a given sourceType/targetType pair.
   * First queries this ConversionService's converter cache.
   * On a cache miss, then performs an exhaustive search for a matching converter.
   * If no converter matches, returns the default converter.
   *
   * @param sourceObject the source  to convert from
   * @param targetType the target type to convert to
   * @return the generic converter that will perform the conversion,
   * or {@code null} if no suitable converter was found
   */
  @Nullable
  default GenericConverter getConverter(Object sourceObject, Class<?> targetType) {
    return getConverter(sourceObject.getClass(), targetType);
  }

  /**
   * Hook method to lookup the converter for a given sourceType/targetType pair.
   * First queries this ConversionService's converter cache.
   * On a cache miss, then performs an exhaustive search for a matching converter.
   * If no converter matches, returns the default converter.
   *
   * @param sourceType the source type to convert from
   * @param targetType the target type to convert to
   * @return the generic converter that will perform the conversion,
   * or {@code null} if no suitable converter was found
   */
  @Nullable
  GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType);

  /**
   * Hook method to lookup the converter for a given sourceType/targetType pair.
   * First queries this ConversionService's converter cache.
   * On a cache miss, then performs an exhaustive search for a matching converter.
   * If no converter matches, returns the default converter.
   *
   * @param sourceType the source type to convert from
   * @param targetType the target type to convert to
   * @return the generic converter that will perform the conversion,
   * or {@code null} if no suitable converter was found
   */
  @Nullable
  default GenericConverter getConverter(Class<?> sourceType, Class<?> targetType) {
    return getConverter(TypeDescriptor.valueOf(sourceType), TypeDescriptor.valueOf(targetType));
  }

  /**
   * Return {@code true} if objects of {@code sourceType} can be converted to the {@code targetType}.
   * <p>If this method returns {@code true}, it means {@link #convert(Object, Class)} is capable
   * of converting an instance of {@code sourceType} to {@code targetType}.
   * <p>Special note on collections, arrays, and maps types:
   * For conversion between collection, array, and map types, this method will return {@code true}
   * even though a convert invocation may still generate a {@link ConversionException} if the
   * underlying elements are not convertible. Callers are expected to handle this exceptional case
   * when working with collections and maps.
   *
   * @param sourceType the source type to convert from (may be {@code null} if source is {@code null})
   * @param targetType the target type to convert to (required)
   * @return {@code true} if a conversion can be performed, {@code false} if not
   * @throws IllegalArgumentException if {@code targetType} is {@code null}
   */
  boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);

  /**
   * Return {@code true} if objects of {@code sourceType} can be converted to the {@code targetType}.
   * The TypeDescriptors provide additional context about the source and target locations
   * where conversion would occur, often object fields or property locations.
   * <p>If this method returns {@code true}, it means {@link #convert(Object, TypeDescriptor, TypeDescriptor)}
   * is capable of converting an instance of {@code sourceType} to {@code targetType}.
   * <p>Special note on collections, arrays, and maps types:
   * For conversion between collection, array, and map types, this method will return {@code true}
   * even though a convert invocation may still generate a {@link ConversionException} if the
   * underlying elements are not convertible. Callers are expected to handle this exceptional case
   * when working with collections and maps.
   *
   * @param sourceType context about the source type to convert from
   * (may be {@code null} if source is {@code null})
   * @param targetType context about the target type to convert to (required)
   * @return {@code true} if a conversion can be performed between the source and target types,
   * {@code false} if not
   * @throws IllegalArgumentException if {@code targetType} is {@code null}
   */
  boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

  /**
   * Convert the given {@code source} to the specified {@code targetType}.
   *
   * @param source the source object to convert (may be {@code null})
   * @param targetType the target type to convert to (required)
   * @return the converted object, an instance of targetType
   * @throws ConversionException if a conversion exception occurred
   * @throws IllegalArgumentException if targetType is {@code null}
   */
  @Nullable
  <T> T convert(@Nullable Object source, Class<T> targetType);

  /**
   * Convert the given {@code source} to the specified {@code targetType}.
   *
   * @param source the source object to convert (may be {@code null})
   * @param targetType the target type to convert to (required)
   * @return the converted object, an instance of targetType
   * @throws ConversionException if a conversion exception occurred
   * @throws IllegalArgumentException if targetType is {@code null}
   */
  @Nullable
  <T> T convert(@Nullable Object source, TypeDescriptor targetType);

  /**
   * Convert the given {@code source} to the specified {@code targetType}.
   * The TypeDescriptors provide additional context about the source and target locations
   * where conversion will occur, often object fields or property locations.
   *
   * @param source the source object to convert (may be {@code null})
   * @param sourceType context about the source type to convert from
   * (may be {@code null} if source is {@code null})
   * @param targetType context about the target type to convert to (required)
   * @return the converted object, an instance of {@link TypeDescriptor#getObjectType() targetType}
   * @throws ConversionException if a conversion exception occurred
   * @throws IllegalArgumentException if targetType is {@code null},
   * or {@code sourceType} is {@code null} but source is not {@code null}
   */
  @Nullable
  Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}
