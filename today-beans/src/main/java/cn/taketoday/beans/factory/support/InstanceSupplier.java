/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.ThrowingBiFunction;
import cn.taketoday.util.function.ThrowingSupplier;

/**
 * Specialized {@link Supplier} that can be set on a
 * {@link AbstractBeanDefinition#setInstanceSupplier(Supplier) BeanDefinition}
 * when details about the {@link RegisteredBean registered bean} are needed to
 * supply the instance.
 *
 * @param <T> the type of instance supplied by this supplier
 * @author Phillip Webb
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
  T get(RegisteredBean registeredBean) throws Exception;

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
      public V get(RegisteredBean registeredBean) throws Exception {
        return after.applyWithException(registeredBean, InstanceSupplier.this.get(registeredBean));
      }

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
      public T get(RegisteredBean registeredBean) throws Exception {
        return supplier.getWithException();
      }

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
