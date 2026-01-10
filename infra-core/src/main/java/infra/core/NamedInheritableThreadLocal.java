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
