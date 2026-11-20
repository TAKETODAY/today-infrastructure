/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.result;

import org.jspecify.annotations.Nullable;

import infra.web.ReturnValueHandler;

/**
 * Extension of {@link ReturnValueHandler} with a combined support check for
 * a request handler and its return value.
 *
 * <p>Override {@link #supportsHandler(Object, Object)} when support depends on
 * both handler metadata and the actual result, for example to distinguish a
 * {@code null} result from a {@code void} method from one returned by a method
 * with a non-void return type.
 *
 * <p>{@link ReturnValueHandler#select(Iterable, Object, Object)} uses the combined
 * check when a non-null handler is supplied and the return value is not
 * {@link #NONE_RETURN_VALUE}. For value-only or handler-only selection, the
 * corresponding single-argument support method is used instead. Override those
 * methods as needed; overriding the combined check does not affect them.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ReturnValueHandler
 * @see ReturnValueHandler#supportsHandler(Object)
 * @see ReturnValueHandler#supportsReturnValue(Object)
 * @since 4.0 2022/4/24 22:26
 */
public interface SmartReturnValueHandler extends ReturnValueHandler {

  /**
   * Determine whether this implementation supports the given handler and
   * return-value combination.
   *
   * <p>The default implementation returns {@code true} if either
   * {@link #supportsHandler(Object)} or {@link #supportsReturnValue(Object)}
   * returns {@code true}, evaluating the return-value check only if the
   * handler check returns {@code false}.
   *
   * @param handler the request handler to check (never {@code null})
   * @param returnValue the return value to be checked for support; may be {@code null}
   * @return {@code true} if the combination is supported, {@code false} otherwise
   */
  default boolean supportsHandler(Object handler, @Nullable Object returnValue) {
    return supportsHandler(handler) || supportsReturnValue(returnValue);
  }

  /**
   * Determine whether this implementation supports the given handler alone.
   *
   * <p>The default implementation returns {@code false}. Override this method
   * to participate in handler-only selection or to provide handler-based
   * support for the default combined check.
   *
   * @param handler the request handler to check (never {@code null})
   * @return {@code true} if the handler is supported, {@code false} otherwise
   */
  @Override
  default boolean supportsHandler(Object handler) {
    return false;
  }

}
