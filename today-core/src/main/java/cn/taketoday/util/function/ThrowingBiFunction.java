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

/**
 * A {@link BiFunction} that allows invocation of code that throws a checked
 * exception.
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
public interface ThrowingBiFunction<T, U, R> extends BiFunction<T, U, R> {

  /**
   * Applies this function to the given argument, possibly throwing a checked
   * exception.
   *
   * @param t the first function argument
   * @param u the second function argument
   * @return the function result
   * @throws Exception on error
   */
  R applyWithException(T t, U u) throws Exception;

  /**
   * Default {@link BiFunction#apply(Object, Object)} that wraps any thrown
   * checked exceptions (by default in a {@link RuntimeException}).
   *
   * @param t the first function argument
   * @param u the second function argument
   * @return the function result
   * @see java.util.function.BiFunction#apply(Object, Object)
   */
  @Override
  default R apply(T t, U u) {
    return apply(t, u, RuntimeException::new);
  }

  /**
   * Applies this function to the given argument, wrapping any thrown checked
   * exceptions using the given {@code exceptionWrapper}.
   *
   * @param t the first function argument
   * @param u the second function argument
   * @param exceptionWrapper {@link BiFunction} that wraps the given message
   * and checked exception into a runtime exception
   * @return a result
   */
  default R apply(T t, U u, BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
    try {
      return applyWithException(t, u);
    }
    catch (RuntimeException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw exceptionWrapper.apply(ex.getMessage(), ex);
    }
  }

  /**
   * Return a new {@link ThrowingBiFunction} where the
   * {@link #apply(Object, Object)} method wraps any thrown checked exceptions
   * using the given {@code exceptionWrapper}.
   *
   * @param exceptionWrapper {@link BiFunction} that wraps the given message
   * and checked exception into a runtime exception
   * @return the replacement {@link ThrowingBiFunction} instance
   */
  default ThrowingBiFunction<T, U, R> throwing(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
    return new ThrowingBiFunction<>() {

      @Override
      public R applyWithException(T t, U u) throws Exception {
        return ThrowingBiFunction.this.applyWithException(t, u);
      }

      @Override
      public R apply(T t, U u) {
        return apply(t, u, exceptionWrapper);
      }

    };
  }

  /**
   * Lambda friendly convenience method that can be used to create a
   * {@link ThrowingBiFunction} where the {@link #apply(Object, Object)}
   * method wraps any checked exception thrown by the supplied lambda expression
   * or method reference.
   * <p>This method can be especially useful when working with method references.
   * It allows you to easily convert a method that throws a checked exception
   * into an instance compatible with a regular {@link BiFunction}.
   * <p>For example:
   * <pre class="code">
   * map.replaceAll(ThrowingBiFunction.of(Example::methodThatCanThrowCheckedException));
   * </pre>
   *
   * @param <T> the type of the first argument to the function
   * @param <U> the type of the second argument to the function
   * @param <R> the type of the result of the function
   * @param function the source function
   * @return a new {@link ThrowingFunction} instance
   */
  static <T, U, R> ThrowingBiFunction<T, U, R> of(ThrowingBiFunction<T, U, R> function) {
    return function;
  }

  /**
   * Lambda friendly convenience method that can be used to create a
   * {@link ThrowingBiFunction} where the {@link #apply(Object, Object)}
   * method wraps any thrown checked exceptions using the given
   * {@code exceptionWrapper}.
   * <p>This method can be especially useful when working with method references.
   * It allows you to easily convert a method that throws a checked exception
   * into an instance compatible with a regular {@link BiFunction}.
   * <p>For example:
   * <pre class="code">
   * map.replaceAll(ThrowingBiFunction.of(Example::methodThatCanThrowCheckedException, IllegalStateException::new));
   * </pre>
   *
   * @param <T> the type of the first argument to the function
   * @param <U> the type of the second argument to the function
   * @param <R> the type of the result of the function
   * @param function the source function
   * @param exceptionWrapper the exception wrapper to use
   * @return a new {@link ThrowingFunction} instance
   */
  static <T, U, R> ThrowingBiFunction<T, U, R> of(ThrowingBiFunction<T, U, R> function,
          BiFunction<String, Exception, RuntimeException> exceptionWrapper) {

    return function.throwing(exceptionWrapper);
  }

}
