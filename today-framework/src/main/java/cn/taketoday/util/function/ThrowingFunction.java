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
import java.util.function.Function;

/**
 * A {@link Function} that allows invocation of code that throws a checked
 * exception.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> extends Function<T, R> {

  /**
   * Applies this function to the given argument, possibly throwing a checked
   * exception.
   *
   * @param t the function argument
   * @return the function result
   * @throws Exception on error
   */
  R applyWithException(T t) throws Exception;

  /**
   * Default {@link Function#apply(Object)} that wraps any thrown checked
   * exceptions (by default in a {@link RuntimeException}).
   *
   * @see Function#apply(Object)
   */
  @Override
  default R apply(T t) {
    return apply(t, RuntimeException::new);
  }

  /**
   * Applies this function to the given argument, wrapping any thrown checked
   * exceptions using the given {@code exceptionWrapper}.
   *
   * @param exceptionWrapper {@link BiFunction} that wraps the given message
   * and checked exception into a runtime exception
   * @return a result
   */
  default R apply(T t, BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
    try {
      return applyWithException(t);
    }
    catch (RuntimeException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw exceptionWrapper.apply(ex.getMessage(), ex);
    }
  }

  /**
   * Return a new {@link ThrowingFunction} where the {@link #apply(Object)}
   * method wraps any thrown checked exceptions using the given
   * {@code exceptionWrapper}.
   *
   * @param exceptionWrapper {@link BiFunction} that wraps the given message
   * and checked exception into a runtime exception
   * @return the replacement {@link ThrowingFunction} instance
   */
  default ThrowingFunction<T, R> throwing(BiFunction<String, Exception, RuntimeException> exceptionWrapper) {
    return new ThrowingFunction<>() {

      @Override
      public R applyWithException(T t) throws Exception {
        return ThrowingFunction.this.applyWithException(t);
      }

      @Override
      public R apply(T t) {
        return apply(t, exceptionWrapper);
      }

    };
  }

  /**
   * Lambda friendly convenience method that can be used to create
   * {@link ThrowingFunction} where the {@link #apply(Object)} method wraps
   * any thrown checked exceptions using the given {@code exceptionWrapper}.
   *
   * @param <T> the type of the input to the function
   * @param <R> the type of the result of the function
   * @param function the source function
   * @return a new {@link ThrowingFunction} instance
   */
  static <T, R> ThrowingFunction<T, R> of(ThrowingFunction<T, R> function) {
    return function;
  }

  /**
   * Lambda friendly convenience method that can be used to create
   * {@link ThrowingFunction} where the {@link #apply(Object)} method wraps
   * any thrown checked exceptions using the given {@code exceptionWrapper}.
   *
   * @param <T> the type of the input to the function
   * @param <R> the type of the result of the function
   * @param function the source function
   * @param exceptionWrapper the exception wrapper to use
   * @return a new {@link ThrowingFunction} instance
   */
  static <T, R> ThrowingFunction<T, R> of(ThrowingFunction<T, R> function,
          BiFunction<String, Exception, RuntimeException> exceptionWrapper) {

    return function.throwing(exceptionWrapper);
  }

}
