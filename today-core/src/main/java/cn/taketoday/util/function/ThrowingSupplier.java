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

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A {@link Supplier} that allows invocation of code that throws a checked
 * exception.
 *
 * @param <T> the type of results supplied by this supplier
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
public interface ThrowingSupplier<T> extends Supplier<T> {

  /**
   * Gets a result, possibly throwing a checked exception.
   *
   * @return a result
   * @throws Exception on error
   */
  T getWithException() throws Exception;

  /**
   * Default {@link Supplier#get()} that wraps any thrown checked exceptions
   * (by default in a {@link RuntimeException}).
   *
   * @see Supplier#get()
   */
  @Override
  default T get() {
    return get(RuntimeException::new);
  }

  /**
   * Gets a result, wrapping any thrown checked exceptions using the given
   * {@code exceptionWrapper}.
   *
   * @param exceptionWrapper {@link BiFunction} that wraps the given message
   * and checked exception into a runtime exception
   * @return a result
   */
  default T get(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
    try {
      return getWithException();
    }
    catch (RuntimeException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw exceptionWrapper.apply(ex.getMessage(), ex);
    }
  }

  /**
   * Return a new {@link ThrowingSupplier} where the {@link #get()} method
   * wraps any thrown checked exceptions using the given
   * {@code exceptionWrapper}.
   *
   * @param exceptionWrapper {@link BiFunction} that wraps the given message
   * and checked exception into a runtime exception
   * @return the replacement {@link ThrowingSupplier} instance
   */
  default ThrowingSupplier<T> throwing(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
    return new ThrowingSupplier<>() {

      @Override
      public T getWithException() throws Exception {
        return ThrowingSupplier.this.getWithException();
      }

      @Override
      public T get() {
        return get(exceptionWrapper);
      }

    };
  }

  /**
   * Lambda friendly convenience method that can be used to create
   * {@link ThrowingSupplier} where the {@link #get()} method wraps any
   * thrown checked exceptions.
   *
   * @param <T> the type of results supplied by this supplier
   * @param supplier the source supplier
   * @return a new {@link ThrowingSupplier} instance
   */
  static <T> ThrowingSupplier<T> of(ThrowingSupplier<T> supplier) {
    return supplier;
  }

  /**
   * Lambda friendly convenience method that can be used to create
   * {@link ThrowingSupplier} where the {@link #get()} method wraps any
   * thrown checked exceptions using the given {@code exceptionWrapper}.
   *
   * @param <T> the type of results supplied by this supplier
   * @param supplier the source supplier
   * @param exceptionWrapper the exception wrapper to use
   * @return a new {@link ThrowingSupplier} instance
   */
  static <T> ThrowingSupplier<T> of(ThrowingSupplier<T> supplier,
          BiFunction<String, Exception, RuntimeException> exceptionWrapper) {

    return supplier.throwing(exceptionWrapper);
  }

}
