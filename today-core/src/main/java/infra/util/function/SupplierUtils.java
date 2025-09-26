/*
 * Copyright 2017 - 2025 the original author or authors.
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

  /**
   * Resolve the given {@code Supplier}, getting its result or immediately
   * returning {@code null} if the supplier itself was {@code null}.
   *
   * @param supplier the supplier to resolve
   * @return the supplier's result, or {@code null} if none
   */
  @Nullable
  public static <T> T resolve(@Nullable Supplier<T> supplier) {
    return (supplier != null ? supplier.get() : null);
  }

  /**
   * Resolve a given {@code Supplier}, getting its result or immediately
   * returning the given Object as-is if not a {@code Supplier}.
   *
   * @param candidate the candidate to resolve (potentially a {@code Supplier})
   * @return a supplier's result or the given Object as-is
   */
  @Nullable
  public static Object resolve(@Nullable Object candidate) {
    return (candidate instanceof Supplier<?> supplier ? supplier.get() : candidate);
  }

}
