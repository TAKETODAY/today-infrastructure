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

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.lang.Contract;
import infra.web.handler.HandlerExecutionChain;

/**
 * A utility interface designed to provide unwrapping capabilities for handler objects.
 * Implementations of this interface are expected to encapsulate a raw handler object,
 * which can be retrieved using the {@link #getRawHandler()} method.
 *
 * <p>This interface also provides a static utility method {@link #unwrap(Object)} to
 * simplify the process of extracting the raw handler from potentially wrapped objects.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * HandlerWrapper wrapper = new HandlerExecutionChain(new MyHandler());
 *
 * // Retrieve the raw handler directly
 * Object rawHandler = wrapper.getRawHandler();
 *
 * // Use the static unwrap method to extract the raw handler
 * Object handler = HandlerWrapper.unwrap(wrapper);
 * if (handler instanceof MyHandler myHandler) {
 *   myHandler.performAction();
 * }
 * }</pre>
 *
 * <p>The {@link #unwrap(Object)} method is particularly useful when dealing with
 * objects that may or may not implement the {@link HandlerWrapper} interface. It
 * ensures safe extraction of the raw handler without requiring explicit type checks.
 *
 * <p><b>Note:</b> The behavior of the {@link #unwrap(Object)} method is governed by
 * the {@link Contract} annotation, ensuring that null inputs result in null outputs,
 * and non-null inputs always produce non-null results.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HandlerExecutionChain
 * @since 4.0 2022/12/7 23:06
 */
public interface HandlerWrapper {

  /**
   * Retrieves the raw handler object encapsulated by this wrapper.
   *
   * <p>This method is useful when you need direct access to the underlying handler
   * without any additional wrapping or decoration. It can be particularly helpful
   * in scenarios where the handler is being passed through layers of abstraction.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * HandlerWrapper wrapper = new HandlerExecutionChain(new MyHandler());
   *
   * // Retrieve the raw handler using getRawHandler
   * Object rawHandler = wrapper.getRawHandler();
   *
   * if (rawHandler instanceof MyHandler myHandler) {
   *   myHandler.performAction();
   * }
   * }</pre>
   *
   * <p>In the example above, the {@code getRawHandler} method is used to extract
   * the raw handler from a {@code HandlerExecutionChain}, allowing direct interaction
   * with the underlying {@code MyHandler} instance.
   *
   * @return the raw handler object encapsulated by this wrapper
   */
  Object getRawHandler();

  /**
   * Unwraps the given handler object if it is an instance of {@link HandlerWrapper}.
   * If the provided handler is not a {@code HandlerWrapper}, it is returned as-is.
   *
   * <p>This method is particularly useful when dealing with objects that may or may not
   * implement the {@code HandlerWrapper} interface. It ensures safe extraction of the
   * raw handler without requiring explicit type checks.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * HandlerWrapper wrapper = new HandlerExecutionChain(new MyHandler());
   *
   * // Use the unwrap method to extract the raw handler
   * Object handler = HandlerWrapper.unwrap(wrapper);
   *
   * if (handler instanceof MyHandler myHandler) {
   *   myHandler.performAction();
   * }
   *
   * // Directly pass a non-wrapped handler
   * MyHandler directHandler = new MyHandler();
   * Object unwrapped = HandlerWrapper.unwrap(directHandler);
   *
   * if (unwrapped instanceof MyHandler myHandler) {
   *   myHandler.performAction();
   * }
   * }</pre>
   *
   * <p>In the example above, the {@code unwrap} method is used to extract the raw handler
   * from a {@code HandlerExecutionChain} and to handle a non-wrapped handler seamlessly.
   *
   * @param handler the handler object to unwrap, which may be {@code null} or an instance
   * of {@code HandlerWrapper}
   * @return the raw handler object if the input is a {@code HandlerWrapper}, or the input
   * itself if it is not a {@code HandlerWrapper}. Returns {@code null} if the input
   * is {@code null}.
   */
  @Nullable
  @Contract("null -> null; !null -> !null")
  static Object unwrap(@Nullable Object handler) {
    if (handler instanceof HandlerWrapper wrapper) {
      return wrapper.getRawHandler();
    }
    return handler;
  }

}
