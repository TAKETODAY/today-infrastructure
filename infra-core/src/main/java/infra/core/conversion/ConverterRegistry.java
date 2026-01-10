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

/**
 * For registering converters with a type conversion system.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY 2021/3/21 17:55
 * @since 3.0
 */
public interface ConverterRegistry {

  /**
   * Add a plain converter to this registry.
   * The convertible source/target type pair is derived from the Converter's parameterized types.
   *
   * @throws IllegalArgumentException if the parameterized types could not be resolved
   */
  void addConverter(Converter<?, ?> converter);

  /**
   * Add a plain converter to this registry.
   * The convertible source/target type pair is specified explicitly.
   * <p>Allows for a Converter to be reused for multiple distinct pairs without
   * having to create a Converter class for each pair.
   */
  <S, T> void addConverter(
          Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter);

  /**
   * Add a generic converter to this registry.
   *
   * @since 4.0
   */
  void addConverter(GenericConverter converter);

  /**
   * Add a ranged converter factory to this registry.
   * The convertible source/target type pair is derived from the ConverterFactory's parameterized types.
   *
   * @throws IllegalArgumentException if the parameterized types could not be resolved
   * @since 4.0
   */
  void addConverterFactory(ConverterFactory<?, ?> factory);

  /**
   * Remove any converters from {@code sourceType} to {@code targetType}.
   *
   * @param sourceType the source type
   * @param targetType the target type
   * @since 4.0
   */
  void removeConvertible(Class<?> sourceType, Class<?> targetType);

  // static

  /**
   * Register the given Converter objects with the given target ConverterRegistry.
   *
   * @param converters the converter objects: implementing {@link Converter},
   * {@link ConverterFactory}, or {@link GenericConverter}
   * @param registry the target registry
   * @since 4.0
   */
  static void registerConverters(@Nullable Set<?> converters, ConverterRegistry registry) {
    if (converters != null) {
      for (Object converter : converters) {
        if (converter instanceof GenericConverter genericConverter) {
          registry.addConverter(genericConverter);
        }
        else if (converter instanceof Converter<?, ?> c) {
          registry.addConverter(c);
        }
        else if (converter instanceof ConverterFactory<?, ?> cf) {
          registry.addConverterFactory(cf);
        }
        else {
          throw new IllegalArgumentException("Each converter object must implement one of the " +
                  "Converter, ConverterFactory, or GenericConverter interfaces");
        }
      }
    }
  }
}
