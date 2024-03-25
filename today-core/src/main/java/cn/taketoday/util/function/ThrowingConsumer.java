/*
 * Copyright 2017 - 2024 the original author or authors.
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
   * @throws Throwable on error
   */
  void acceptWithException(T t) throws Throwable;

  /**
   * Default {@link Consumer#accept(Object)} that wraps any thrown checked
   * exceptions (by default in a {@link RuntimeException}).
   *
   * @see java.util.function.Consumer#accept(Object)
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
  default void accept(T t, BiFunction<String, Throwable, RuntimeException> exceptionWrapper) {
    try {
      acceptWithException(t);
    }
    catch (RuntimeException ex) {
      throw ex;
    }
    catch (Throwable ex) {
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
  default ThrowingConsumer<T> throwing(BiFunction<String, Throwable, RuntimeException> exceptionWrapper) {
    return new ThrowingConsumer<>() {

      @Override
      public void acceptWithException(T t) throws Throwable {
        ThrowingConsumer.this.acceptWithException(t);
      }

      @Override
      public void accept(T t) {
        accept(t, exceptionWrapper);
      }

    };
  }

  /**
   * Lambda friendly convenience method that can be used to create a
   * {@link ThrowingConsumer} where the {@link #accept(Object)} method wraps
   * any checked exception thrown by the supplied lambda expression or method
   * reference.
   * <p>This method can be especially useful when working with method references.
   * It allows you to easily convert a method that throws a checked exception
   * into an instance compatible with a regular {@link Consumer}.
   * <p>For example:
   * <pre class="code">
   * list.forEach(ThrowingConsumer.of(Example::methodThatCanThrowCheckedException));
   * </pre>
   *
   * @param <T> the type of the input to the operation
   * @param consumer the source consumer
   * @return a new {@link ThrowingConsumer} instance
   */
  static <T> ThrowingConsumer<T> of(ThrowingConsumer<T> consumer) {
    return consumer;
  }

  /**
   * Lambda friendly convenience method that can be used to create a
   * {@link ThrowingConsumer} where the {@link #accept(Object)} method wraps
   * any thrown checked exceptions using the given {@code exceptionWrapper}.
   * <p>This method can be especially useful when working with method references.
   * It allows you to easily convert a method that throws a checked exception
   * into an instance compatible with a regular {@link Consumer}.
   * <p>For example:
   * <pre class="code">
   * list.forEach(ThrowingConsumer.of(Example::methodThatCanThrowCheckedException, IllegalStateException::new));
   * </pre>
   *
   * @param <T> the type of the input to the operation
   * @param consumer the source consumer
   * @param exceptionWrapper the exception wrapper to use
   * @return a new {@link ThrowingConsumer} instance
   */
  static <T> ThrowingConsumer<T> of(ThrowingConsumer<T> consumer,
          BiFunction<String, Throwable, RuntimeException> exceptionWrapper) {

    return consumer.throwing(exceptionWrapper);
  }

}
