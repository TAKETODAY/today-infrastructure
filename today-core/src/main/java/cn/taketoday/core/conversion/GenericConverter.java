/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.conversion;

import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Generic converter interface for converting between two or more types.
 *
 * <p>This is the most flexible of the Converter SPI interfaces, but also the most complex.
 * It is flexible in that a GenericConverter may support converting between multiple source/target
 * type pairs (see {@link #getConvertibleTypes()}. In addition, GenericConverter implementations
 * have access to source/target {@link TypeDescriptor field context} during the type conversion
 * process. This allows for resolving source and target field metadata such as annotations and
 * generics information, which can be used to influence the conversion logic.
 *
 * <p>This interface should generally not be used when the simpler {@link Converter} or
 * {@link ConverterFactory} interface is sufficient.
 *
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TypeDescriptor
 * @see Converter
 * @see ConverterFactory
 * @see ConditionalConverter
 * @since 4.0
 */
public interface GenericConverter {

  /**
   * Return the source and target types that this converter can convert between.
   * <p>Each entry is a convertible source-to-target type pair.
   * <p>For {@link ConditionalConverter conditional converters} this method may return
   * {@code null} to indicate all source-to-target pairs should be considered.
   */
  @Nullable
  Set<ConvertiblePair> getConvertibleTypes();

  /**
   * Convert the source object to the targetType described by the {@code TypeDescriptor}.
   *
   * @param source the source object to convert (may be {@code null})
   * @param sourceType the type descriptor of the field we are converting from
   * @param targetType the type descriptor of the field we are converting to
   * @return the converted object
   */
  @Nullable
  Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType);

  /**
   * Holder for a source-to-target class pair.
   */
  final class ConvertiblePair {
    public final Class<?> sourceType;

    public final Class<?> targetType;

    /**
     * Create a new source-to-target pair.
     *
     * @param sourceType the source type
     * @param targetType the target type
     */
    public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
      Assert.notNull(sourceType, "Source type is required");
      Assert.notNull(targetType, "Target type is required");
      this.sourceType = sourceType;
      this.targetType = targetType;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (other instanceof ConvertiblePair otherPair) {
        return sourceType == otherPair.sourceType
                && targetType == otherPair.targetType;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return (this.sourceType.hashCode() * 31 + this.targetType.hashCode());
    }

    @Override
    public String toString() {
      return (this.sourceType.getName() + " -> " + this.targetType.getName());
    }
  }

}
