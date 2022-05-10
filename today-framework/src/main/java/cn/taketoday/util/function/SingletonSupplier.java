/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.util.function;

import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A {@link java.util.function.Supplier} decorator that caches a singleton result and
 * makes it available from {@link #get()} (nullable) and {@link #obtain()} (null-safe).
 *
 * <p>A {@code SingletonSupplier} can be constructed via {@code of} factory methods
 * or via constructors that provide a default supplier as a fallback. This is
 * particularly useful for method reference suppliers, falling back to a default
 * supplier for a method that returned {@code null} and caching the result.
 *
 * <p>
 * From spring
 * </p>
 *
 * @param <T> the type of results supplied by this supplier
 * @author Juergen Hoeller
 * @author TODAY 2021/3/25 11:38
 * @since 3.0
 */
public class SingletonSupplier<T> implements Supplier<T> {

  @Nullable
  private volatile T singletonInstance;

  @Nullable
  private final Supplier<? extends T> defaultSupplier;

  @Nullable
  private final Supplier<? extends T> instanceSupplier;

  /**
   * Build a {@code SingletonSupplier} with the given singleton instance
   * and a default supplier for the case when the instance is {@code null}.
   *
   * @param instance the singleton instance (potentially {@code null})
   * @param defaultSupplier the default supplier as a fallback
   */
  public SingletonSupplier(T instance, Supplier<? extends T> defaultSupplier) {
    this.instanceSupplier = null;
    this.singletonInstance = instance;
    this.defaultSupplier = defaultSupplier;
  }

  /**
   * Build a {@code SingletonSupplier} with the given instance supplier
   * and a default supplier for the case when the instance is {@code null}.
   *
   * @param instanceSupplier the immediate instance supplier
   * @param defaultSupplier the default supplier as a fallback
   */
  public SingletonSupplier(Supplier<? extends T> instanceSupplier, Supplier<? extends T> defaultSupplier) {
    this.defaultSupplier = defaultSupplier;
    this.instanceSupplier = instanceSupplier;
  }

  private SingletonSupplier(Supplier<? extends T> supplier) {
    this.defaultSupplier = null;
    this.instanceSupplier = supplier;
  }

  private SingletonSupplier(T singletonInstance) {
    this.defaultSupplier = null;
    this.instanceSupplier = null;
    this.singletonInstance = singletonInstance;
  }

  /**
   * Get the shared singleton instance for this supplier.
   *
   * @return the singleton instance (or {@code null} if none)
   */
  @Override
  @Nullable
  public T get() {
    T instance = this.singletonInstance;
    if (instance == null) {
      synchronized(this) {
        instance = this.singletonInstance;
        if (instance == null) {
          if (instanceSupplier != null) {
            instance = instanceSupplier.get();
          }
          if (instance == null && defaultSupplier != null) {
            instance = defaultSupplier.get();
          }
          this.singletonInstance = instance;
        }
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
  public static <T> SingletonSupplier<T> valueOf(T instance) {
    return new SingletonSupplier<>(instance);
  }

  /**
   * Build a {@code SingletonSupplier} with the given singleton instance.
   *
   * @param instance the singleton instance (potentially {@code null})
   * @return the singleton supplier, or {@code null} if the instance was {@code null}
   */
  @Nullable
  public static <T> SingletonSupplier<T> ofNullable(@Nullable T instance) {
    return (instance != null ? new SingletonSupplier<>(instance) : null);
  }

  /**
   * Build a {@code SingletonSupplier} with the given supplier.
   *
   * @param supplier the instance supplier (never {@code null})
   * @return the singleton supplier (never {@code null})
   */
  public static <T> SingletonSupplier<T> from(Supplier<T> supplier) {
    return new SingletonSupplier<>(supplier);
  }

  /**
   * Build a {@code SingletonSupplier} with the given supplier.
   *
   * @param supplier the instance supplier (potentially {@code null})
   * @return the singleton supplier, or {@code null} if the instance supplier was {@code null}
   */
  @Nullable
  public static <T> SingletonSupplier<T> ofNullable(@Nullable Supplier<T> supplier) {
    return (supplier != null ? new SingletonSupplier<>(supplier) : null);
  }

}
