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

package cn.taketoday.beans.factory.support;

import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
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
   * Gets the supplied instance.
   *
   * @param registeredBean the registered bean requesting the instance
   * @return the supplied instance
   * @throws Exception on error
   */
  T get(RegisteredBean registeredBean) throws Exception;

  /**
   * Return a composed instance supplier that first obtains the instance from
   * this supplier, and then applied the {@code after} function to obtain the
   * result.
   *
   * @param <V> the type of output of the {@code after} function, and of the
   * composed function
   * @param after the function to apply after the instance is obtained
   * @return a composed instance supplier
   */
  default <V> InstanceSupplier<V> andThen(
          ThrowingBiFunction<RegisteredBean, ? super T, ? extends V> after) {
    Assert.notNull(after, "After must not be null");
    return registeredBean -> after.applyWithException(registeredBean,
            get(registeredBean));
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
    Assert.notNull(supplier, "Supplier must not be null");
    if (supplier instanceof InstanceSupplier<T> instanceSupplier) {
      return instanceSupplier;
    }
    return registeredBean -> supplier.getWithException();
  }

  /**
   * Lambda friendly method that can be used to create a
   * {@link InstanceSupplier} and add post processors in a single call. For
   * example: {@code
   * InstanceSupplier.of(registeredBean -> ...).withPostProcessor(...)}.
   *
   * @param <T> the type of instance supplied by this supplier
   * @param instanceSupplier the source instance supplier
   * @return a new {@link InstanceSupplier}
   */
  static <T> InstanceSupplier<T> of(InstanceSupplier<T> instanceSupplier) {
    Assert.notNull(instanceSupplier, "InstanceSupplier must not be null");
    return instanceSupplier;
  }

}
