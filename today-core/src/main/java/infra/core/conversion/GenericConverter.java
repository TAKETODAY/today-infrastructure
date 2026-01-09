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

package infra.core.conversion;

import org.jspecify.annotations.Nullable;

import java.util.Set;

import infra.core.TypeDescriptor;
import infra.lang.Assert;

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
