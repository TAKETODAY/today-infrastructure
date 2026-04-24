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

package infra.util.function;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Convenience utilities for {@link java.util.function.Supplier} handling.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SingletonSupplier
 * @since 4.0 2022/3/9 20:55
 */
public abstract class SupplierUtils {

  @SuppressWarnings("rawtypes")
  private static final Supplier empty = new AlwaysSupplier<>(null);

  /**
   * Resolve the given {@code Supplier}, getting its result or immediately
   * returning {@code null} if the supplier itself was {@code null}.
   *
   * @param supplier the supplier to resolve
   * @return the supplier's result, or {@code null} if none
   */
  public static <T> @Nullable T resolve(@Nullable Supplier<T> supplier) {
    return supplier != null ? supplier.get() : null;
  }

  /**
   * Resolve a given {@code Supplier}, getting its result or immediately
   * returning the given Object as-is if not a {@code Supplier}.
   *
   * @param candidate the candidate to resolve (potentially a {@code Supplier})
   * @return a supplier's result or the given Object as-is
   */
  public static @Nullable Object resolve(@Nullable Object candidate) {
    return candidate instanceof Supplier<?> supplier ? supplier.get() : candidate;
  }

  /**
   * Return an empty {@code Supplier} that always returns {@code null}.
   *
   * @param <T> the type of results supplied by this supplier
   * @return an empty supplier
   * @since 5.0
   */
  @SuppressWarnings("unchecked")
  public static <T> Supplier<@Nullable T> empty() {
    return empty;
  }

  /**
   * Return a {@code Supplier} that always returns the given value.
   *
   * @param value the value to be returned by the supplier
   * @param <T> the type of results supplied by this supplier
   * @return a supplier that always returns the given value
   * @since 5.0
   */
  @SuppressWarnings("unchecked")
  public static <T extends @Nullable Object> Supplier<@Nullable T> always(T value) {
    if (value == null) {
      return empty;
    }
    return new AlwaysSupplier<>(value);
  }

  private static final class AlwaysSupplier<T extends @Nullable Object> implements Supplier<T> {

    private final T value;

    AlwaysSupplier(T value) {
      this.value = value;
    }

    @Override
    public T get() {
      return value;
    }

  }

}
