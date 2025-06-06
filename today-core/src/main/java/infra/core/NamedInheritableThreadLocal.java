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

package infra.core;

import java.util.function.Supplier;

import infra.lang.Assert;

/**
 * {@link InheritableThreadLocal} subclass that exposes a specified name
 * as {@link #toString()} result (allowing for introspection).
 *
 * @param <T> the value type
 * @author Juergen Hoeller
 * @author TODAY 2021/9/11 12:13
 * @see NamedThreadLocal
 * @since 4.0
 */
public class NamedInheritableThreadLocal<T> extends InheritableThreadLocal<T> {

  private final String name;

  /**
   * Create a new NamedInheritableThreadLocal with the given name.
   *
   * @param name a descriptive name for this ThreadLocal
   */
  public NamedInheritableThreadLocal(String name) {
    Assert.hasText(name, "Name must not be empty");
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }

  /**
   * Creates a thread local variable. The initial value of the variable is
   * determined by invoking the {@code get} method on the {@code Supplier}.
   *
   * @param <S> the type of the thread local's value
   * @param supplier the supplier to be used to determine the initial value
   * @return a new thread local variable
   * @throws NullPointerException if the specified supplier is null
   * @since 4.0
   */
  public static <S> NamedInheritableThreadLocal<S> withInitial(String name, Supplier<? extends S> supplier) {
    Assert.notNull(supplier, "Supplier is required");
    final class Supplied extends NamedInheritableThreadLocal<S> {
      Supplied(String name) {
        super(name);
      }

      @Override
      protected S initialValue() {
        return supplier.get();
      }
    }
    return new Supplied(name);
  }

}
