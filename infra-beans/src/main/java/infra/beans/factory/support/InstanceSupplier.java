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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import infra.lang.Assert;
import infra.util.function.ThrowingBiFunction;
import infra.util.function.ThrowingSupplier;

/**
 * Specialized {@link Supplier} that can be set on a
 * {@link AbstractBeanDefinition#setInstanceSupplier(Supplier) BeanDefinition}
 * when details about the {@link RegisteredBean registered bean} are needed to
 * supply the instance.
 *
 * @param <T> the type of instance supplied by this supplier
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RegisteredBean
 * @since 4.0
 */
@FunctionalInterface
public interface InstanceSupplier<T> extends ThrowingSupplier<T> {

  @Override
  default T getWithException() {
    throw new IllegalStateException("No RegisteredBean parameter provided");
  }

  /**
   * Get the supplied instance.
   *
   * @param registeredBean the registered bean requesting the instance
   * @return the supplied instance
   * @throws Exception on error
   */
  T get(RegisteredBean registeredBean) throws Throwable;

  /**
   * Return the factory method that this supplier uses to create the
   * instance, or {@code null} if it is not known or this supplier uses
   * another means.
   *
   * @return the factory method used to create the instance, or {@code null}
   */
  @Nullable
  default Method getFactoryMethod() {
    return null;
  }

  /**
   * Return a composed instance supplier that first obtains the instance from
   * this supplier and then applies the {@code after} function to obtain the
   * result.
   *
   * @param <V> the type of output of the {@code after} function, and of the
   * composed function
   * @param after the function to apply after the instance is obtained
   * @return a composed instance supplier
   */
  default <V> InstanceSupplier<V> andThen(
          ThrowingBiFunction<RegisteredBean, ? super T, ? extends V> after) {

    Assert.notNull(after, "'after' function is required");
    return new InstanceSupplier<>() {
      @Override
      public V get(RegisteredBean registeredBean) throws Throwable {
        return after.applyWithException(registeredBean, InstanceSupplier.this.get(registeredBean));
      }

      @Nullable
      @Override
      public Method getFactoryMethod() {
        return InstanceSupplier.this.getFactoryMethod();
      }
    };
  }

  /**
   * Factory method to create an {@link InstanceSupplier} from a
   * {@link ThrowingSupplier}.
   *
   * @param <T> the type of instance supplied by this supplier
   * @param supplier the source supplier
   * @return a new {@link InstanceSupplier}
   */
  static <T> InstanceSupplier<T> using(ThrowingSupplier<T> supplier) {
    Assert.notNull(supplier, "Supplier is required");
    if (supplier instanceof InstanceSupplier<T> instanceSupplier) {
      return instanceSupplier;
    }
    return registeredBean -> supplier.getWithException();
  }

  /**
   * Factory method to create an {@link InstanceSupplier} from a
   * {@link ThrowingSupplier}.
   *
   * @param <T> the type of instance supplied by this supplier
   * @param factoryMethod the factory method being used
   * @param supplier the source supplier
   * @return a new {@link InstanceSupplier}
   */
  static <T> InstanceSupplier<T> using(@Nullable Method factoryMethod, ThrowingSupplier<T> supplier) {
    Assert.notNull(supplier, "Supplier is required");

    if (supplier instanceof InstanceSupplier<T> instanceSupplier &&
            instanceSupplier.getFactoryMethod() == factoryMethod) {
      return instanceSupplier;
    }

    return new InstanceSupplier<>() {
      @Override
      public T get(RegisteredBean registeredBean) throws Throwable {
        return supplier.getWithException();
      }

      @Nullable
      @Override
      public Method getFactoryMethod() {
        return factoryMethod;
      }
    };
  }

  /**
   * Lambda friendly method that can be used to create an
   * {@link InstanceSupplier} and add post processors in a single call. For
   * example: {@code InstanceSupplier.of(registeredBean -> ...).andThen(...)}.
   *
   * @param <T> the type of instance supplied by this supplier
   * @param instanceSupplier the source instance supplier
   * @return a new {@link InstanceSupplier}
   */
  static <T> InstanceSupplier<T> of(InstanceSupplier<T> instanceSupplier) {
    Assert.notNull(instanceSupplier, "InstanceSupplier is required");
    return instanceSupplier;
  }

}
