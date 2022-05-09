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
import java.util.function.Consumer;

/**
 * A {@link Consumer} that allows invocation of code that throws a checked
 * exception.
 *
 * @param <T> the type of the input to the operation
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

  /**
   * Performs this operation on the given argument, possibly throwing a
   * checked exception.
   *
   * @param t the input argument
   * @throws Exception on error
   */
  void acceptWithException(T t) throws Exception;

  /**
   * Default {@link Consumer#accept(Object)} that wraps any thrown checked
   * exceptions (by default in a {@link RuntimeException}).
   *
   * @see Consumer#accept(Object)
   */
  @Override
  default void accept(T t) {
    accept(t, RuntimeException::new);
  }

  /**
   * Performs this operation on the given argument, wrapping any thrown
   * checked exceptions using the given {@code exceptionWrapper}.
   *
   * @param exceptionWrapper {@link BiFunction} that wraps the given message
   * and checked exception into a runtime exception
   */
  default void accept(T t, BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
    try {
      acceptWithException(t);
    }
    catch (RuntimeException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw exceptionWrapper.apply(ex.getMessage(), ex);
    }
  }

  /**
   * Return a new {@link ThrowingConsumer} where the {@link #accept(Object)}
   * method wraps any thrown checked exceptions using the given
   * {@code exceptionWrapper}.
   *
   * @param exceptionWrapper {@link BiFunction} that wraps the given message
   * and checked exception into a runtime exception
   * @return the replacement {@link ThrowingConsumer} instance
   */
  default ThrowingConsumer<T> throwing(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
    return new ThrowingConsumer<>() {

      @Override
      public void acceptWithException(T t) throws Exception {
        ThrowingConsumer.this.acceptWithException(t);
      }

      @Override
      public void accept(T t) {
        accept(t, exceptionWrapper);
      }

    };
  }

  /**
   * Lambda friendly convenience method that can be used to create
   * {@link ThrowingConsumer} where the {@link #accept(Object)} method wraps
   * any thrown checked exceptions using the given {@code exceptionWrapper}.
   *
   * @param <T> the type of the input to the operation
   * @param consumer the source consumer
   * @return a new {@link ThrowingConsumer} instance
   */
  static <T> ThrowingConsumer<T> of(ThrowingConsumer<T> consumer) {
    return consumer;
  }

  /**
   * Lambda friendly convenience method that can be used to create
   * {@link ThrowingConsumer} where the {@link #accept(Object)} method wraps
   * any thrown checked exceptions using the given {@code exceptionWrapper}.
   *
   * @param <T> the type of the input to the operation
   * @param consumer the source consumer
   * @param exceptionWrapper the exception wrapper to use
   * @return a new {@link ThrowingConsumer} instance
   */
  static <T> ThrowingConsumer<T> of(ThrowingConsumer<T> consumer,
          BiFunction<String, Exception, RuntimeException> exceptionWrapper) {

    return consumer.throwing(exceptionWrapper);
  }

}
