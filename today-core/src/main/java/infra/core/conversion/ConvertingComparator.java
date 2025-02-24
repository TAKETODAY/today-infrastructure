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

import java.util.Comparator;
import java.util.Map;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.comparator.Comparators;

/**
 * A {@link Comparator} that converts values before they are compared.
 *
 * <p>The specified {@link Converter} will be used to convert each value
 * before it is passed to the underlying {@code Comparator}.
 *
 * @param <S> the source type
 * @param <T> the target type
 * @author Phillip Webb
 * @since 4.0
 */
public class ConvertingComparator<S, T> implements Comparator<S> {

  private final Comparator<T> comparator;

  private final Converter<S, T> converter;

  /**
   * Create a new {@link ConvertingComparator} instance.
   *
   * @param converter the converter
   */
  public ConvertingComparator(Converter<S, T> converter) {
    this(Comparators.comparable(), converter);
  }

  /**
   * Create a new {@link ConvertingComparator} instance.
   *
   * @param comparator the underlying comparator used to compare the converted values
   * @param converter the converter
   */
  public ConvertingComparator(Comparator<T> comparator, Converter<S, T> converter) {
    Assert.notNull(comparator, "Comparator is required");
    Assert.notNull(converter, "Converter is required");
    this.comparator = comparator;
    this.converter = converter;
  }

  /**
   * Create a new {@code ConvertingComparator} instance.
   *
   * @param comparator the underlying comparator
   * @param conversionService the conversion service
   * @param targetType the target type
   */
  public ConvertingComparator(
          Comparator<T> comparator, ConversionService conversionService, Class<? extends T> targetType) {

    this(comparator, new ConversionServiceConverter<>(conversionService, targetType));
  }

  @Override
  public int compare(S o1, S o2) {
    T c1 = this.converter.convert(o1);
    T c2 = this.converter.convert(o2);
    return this.comparator.compare(c1, c2);
  }

  /**
   * Create a new {@link ConvertingComparator} that compares {@linkplain Map.Entry
   * map entries} based on their {@linkplain Map.Entry#getKey() keys}.
   *
   * @param comparator the underlying comparator used to compare keys
   * @return a new {@link ConvertingComparator} instance
   */
  public static <K, V> ConvertingComparator<Map.Entry<K, V>, K> mapEntryKeys(Comparator<K> comparator) {
    return new ConvertingComparator<>(comparator, Map.Entry::getKey);
  }

  /**
   * Create a new {@link ConvertingComparator} that compares {@linkplain Map.Entry
   * map entries} based on their {@linkplain Map.Entry#getValue() values}.
   *
   * @param comparator the underlying comparator used to compare values
   * @return a new {@link ConvertingComparator} instance
   */
  public static <K, V> ConvertingComparator<Map.Entry<K, V>, V> mapEntryValues(Comparator<V> comparator) {
    return new ConvertingComparator<>(comparator, Map.Entry::getValue);
  }

  /**
   * Adapts a {@link ConversionService} and <tt>targetType</tt> to a {@link Converter}.
   */
  private static class ConversionServiceConverter<S, T> implements Converter<S, T> {

    private final ConversionService conversionService;

    private final Class<? extends T> targetType;

    public ConversionServiceConverter(ConversionService conversionService, Class<? extends T> targetType) {
      Assert.notNull(conversionService, "ConversionService is required");
      Assert.notNull(targetType, "TargetType is required");
      this.conversionService = conversionService;
      this.targetType = targetType;
    }

    @Override
    @Nullable
    public T convert(S source) {
      return this.conversionService.convert(source, this.targetType);
    }
  }

}
