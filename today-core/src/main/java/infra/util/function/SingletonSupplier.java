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

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import infra.lang.Assert;
import infra.lang.Contract;

/**
 * A {@link java.util.function.Supplier} decorator that caches a singleton result and
 * makes it available from {@link #get()} (nullable) and {@link #obtain()} (null-safe).
 *
 * <p>A {@code SingletonSupplier} can be constructed via {@code of} factory methods
 * or via constructors that provide a default supplier as a fallback. This is
 * particularly useful for method reference suppliers, falling back to a default
 * supplier for a method that returned {@code null} and caching the result.
 *
 * @param <T> the type of results supplied by this supplier
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/3/25 11:38
 */
public class SingletonSupplier<T extends @Nullable Object> implements Supplier<T> {

  private final @Nullable Supplier<? extends @Nullable T> defaultSupplier;

  private final @Nullable Supplier<? extends @Nullable T> instanceSupplier;

  /**
   * Guards access to write operations on the {@code singletonInstance} and
   * {@code initialized} fields.
   */
  private final ReentrantLock initializationLock = new ReentrantLock();

  private volatile @Nullable T singletonInstance;

  private boolean initialized;

  /**
   * Build a {@code SingletonSupplier} with the given singleton instance
   * and a default supplier for the case when the instance is {@code null}.
   *
   * @param instance the singleton instance (potentially {@code null})
   * @param defaultSupplier the default supplier as a fallback
   */
  public SingletonSupplier(@Nullable T instance, Supplier<? extends @Nullable T> defaultSupplier) {
    this.instanceSupplier = null;
    this.defaultSupplier = defaultSupplier;
    this.singletonInstance = instance;
    this.initialized = (instance != null);
  }

  /**
   * Build a {@code SingletonSupplier} with the given instance supplier
   * and a default supplier for the case when the instance is {@code null}.
   *
   * @param instanceSupplier the immediate instance supplier
   * @param defaultSupplier the default supplier as a fallback
   */
  public SingletonSupplier(@Nullable Supplier<? extends @Nullable T> instanceSupplier, Supplier<? extends @Nullable T> defaultSupplier) {
    this.instanceSupplier = instanceSupplier;
    this.defaultSupplier = defaultSupplier;
  }

  private SingletonSupplier(Supplier<? extends @Nullable T> supplier) {
    this.instanceSupplier = supplier;
    this.defaultSupplier = null;
  }

  private SingletonSupplier(@Nullable T singletonInstance) {
    this.instanceSupplier = null;
    this.defaultSupplier = null;
    this.singletonInstance = singletonInstance;
    this.initialized = (singletonInstance != null);
  }

  /**
   * Get the shared singleton instance for this supplier.
   *
   * @return the singleton instance (or {@code null} if none)
   */
  @Override
  public @Nullable T get() {
    T instance = this.singletonInstance;
    if (instance == null) {
      // Either not initialized yet, or a pre-initialized null value ->
      // specific determination follows within full initialization lock.
      // Pre-initialized null values are rare, so we accept the locking
      // overhead in favor of a defensive fast path for non-null values.
      this.initializationLock.lock();
      try {
        instance = this.singletonInstance;
        if (!this.initialized) {
          if (this.instanceSupplier != null) {
            instance = this.instanceSupplier.get();
          }
          if (instance == null && this.defaultSupplier != null) {
            instance = this.defaultSupplier.get();
          }
          this.singletonInstance = instance;
          this.initialized = true;
        }
      }
      finally {
        this.initializationLock.unlock();
      }
    }
    return instance;
  }

  /**
   * Obtain the shared singleton instance for this supplier.
   *
   * @return the singleton instance (never {@code null})
   * @throws IllegalStateException in case of no instance
   */
  public T obtain() {
    T instance = get();
    Assert.state(instance != null, "No instance from Supplier");
    return instance;
  }

  /**
   * Build a {@code SingletonSupplier} with the given singleton instance.
   *
   * @param instance the singleton instance (never {@code null})
   * @return the singleton supplier (never {@code null})
   */
  public static <T> SingletonSupplier<T> of(T instance) {
    return new SingletonSupplier<>(instance);
  }

  /**
   * Build a {@code SingletonSupplier} with the given singleton instance.
   *
   * @param instance the singleton instance (potentially {@code null})
   * @return the singleton supplier, or {@code null} if the instance was {@code null}
   */
  @Contract("null -> null; !null -> !null")
  public static <T extends @Nullable Object> @Nullable SingletonSupplier<T> ofNullable(@Nullable T instance) {
    return (instance != null ? new SingletonSupplier<>(instance) : null);
  }

  /**
   * Build a {@code SingletonSupplier} with the given supplier.
   *
   * @param supplier the instance supplier (never {@code null})
   * @return the singleton supplier (never {@code null})
   */
  public static <T extends @Nullable Object> SingletonSupplier<T> of(Supplier<T> supplier) {
    return new SingletonSupplier<>(supplier);
  }

  /**
   * Build a {@code SingletonSupplier} with the given supplier.
   *
   * @param supplier the instance supplier (potentially {@code null})
   * @return the singleton supplier, or {@code null} if the instance supplier was {@code null}
   */
  @Contract("null -> null; !null -> !null")
  public static <T extends @Nullable Object> @Nullable SingletonSupplier<T> ofNullable(@Nullable Supplier<T> supplier) {
    return (supplier != null ? new SingletonSupplier<>(supplier) : null);
  }

}
