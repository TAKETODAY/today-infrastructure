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

/**
 * A factory for "ranged" converters that can convert objects from S to subtypes of R.
 *
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 *
 * @param <S> the source type converters created by this factory can convert from
 * @param <R> the target range (or base) type converters created by this factory can convert to;
 * for example {@link Number} for a set of number subtypes.
 * @author Keith Donald
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ConditionalConverter
 * @since 3.0
 */
public interface ConverterFactory<S, R extends @Nullable Object> {

  /**
   * Get the converter to convert from S to target type T, where T is also an instance of R.
   *
   * @param <T> the target type
   * @param targetType the target type to convert to
   * @return a converter from S to T
   */
  <T extends R> Converter<S, T> getConverter(Class<T> targetType);

}
