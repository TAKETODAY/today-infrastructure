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

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.web.handler.method.HandlerMethod;
import infra.web.handler.result.HandlerMethodReturnValueHandler;
import infra.web.handler.result.SmartReturnValueHandler;

/**
 * Strategy for processing return values from HTTP request handlers.
 *
 * <p>Implementations declare support for a handler, a return value, or both,
 * and process supported results within an {@link ServerHttpExchange}. Use
 * {@link SmartReturnValueHandler} when support depends on the combination of
 * a handler and its return value.
 *
 * <p>The {@link #select(Iterable, Object, Object)} utility method selects the
 * first matching candidate in iteration order. Selection does not invoke
 * {@link #handleReturnValue(ServerHttpExchange, Object, Object)}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see SmartReturnValueHandler
 * @see HandlerMethodReturnValueHandler
 * @see HandlerExceptionHandler
 * @see HandlerMethod
 * @since 4.0 2019-07-10 19:22
 */
public interface ReturnValueHandler {

  /**
   * Sentinel indicating that no return value is available for value-based handling.
   * This is the same instance as {@link HttpRequestHandler#NONE_RETURN_VALUE}.
   *
   * <p>Unlike {@code null}, this sentinel disables return-value support checks in
   * {@link #select(Iterable, Object, Object)}. A candidate may still be selected
   * through {@link #supportsHandler(Object)} if a handler is supplied.
   *
   * <p>Compare this sentinel by identity ({@code ==}), not by equality.
   *
   * @see HttpRequestHandler#NONE_RETURN_VALUE
   * @see ReturnValueHandler#supportsReturnValue(Object)
   * @see ReturnValueHandler#select(Iterable, Object, Object)
   */
  Object NONE_RETURN_VALUE = HttpRequestHandler.NONE_RETURN_VALUE;

  /**
   * Determine whether this implementation supports return-value handling for the
   * given request handler.
   *
   * @param handler the request handler to check (never {@code null})
   * @return {@code true} if the handler is supported, {@code false} otherwise
   */
  boolean supportsHandler(Object handler);

  /**
   * Determine whether this implementation supports the given return value.
   *
   * <p>The default implementation returns {@code false}. Override this method
   * to support results based on their type or content, including {@code null}
   * if applicable.
   *
   * @param returnValue the return value to be checked for support; may be {@code null}
   * @return {@code true} if the return value is supported, {@code false} otherwise
   */
  default boolean supportsReturnValue(@Nullable Object returnValue) {
    return false;
  }

  /**
   * Process the return value within the current request context, for example by
   * writing a response or rendering a view.
   *
   * @param exchange the current HTTP request and response context
   * @param handler the handler object associated with the request; may be {@code null}
   * @param returnValue the result to process; may be {@code null} or
   * {@link #NONE_RETURN_VALUE} for handler-only selection
   * @throws Exception if an error occurs while handling the return value
   */
  void handleReturnValue(ServerHttpExchange exchange, @Nullable Object handler, @Nullable Object returnValue)
          throws Exception;

  /**
   * Select the first matching return-value handler in candidate iteration order.
   *
   * <p>Support is checked according to the supplied arguments:
   * <ul>
   * <li>If {@code handler} is non-null and {@code returnValue} is not
   * {@link #NONE_RETURN_VALUE}, a {@link SmartReturnValueHandler} is checked via
   * {@link SmartReturnValueHandler#supportsHandler(Object, Object)}. Other
   * candidates match if either {@link #supportsHandler(Object)} or
   * {@link #supportsReturnValue(Object)} returns {@code true}.</li>
   * <li>If {@code handler} is {@code null} and {@code returnValue} is not
   * {@code NONE_RETURN_VALUE}, only {@code supportsReturnValue} is checked.</li>
   * <li>If {@code returnValue} is {@code NONE_RETURN_VALUE} and {@code handler}
   * is non-null, only {@code supportsHandler(Object)} is checked.</li>
   * </ul>
   *
   * <p>A {@code null} return value participates in support checks like any other
   * value. If no candidate matches, or if {@code handler} is {@code null} and
   * {@code returnValue} is {@code NONE_RETURN_VALUE}, this method returns
   * {@code null}. The selected candidate is not invoked by this method.
   *
   * @param handlers the candidates to evaluate in iteration order (never {@code null})
   * @param handler the request handler to check; may be {@code null}
   * @param returnValue the result to check; may be {@code null} or
   * {@link #NONE_RETURN_VALUE}
   * @return the first matching candidate, or {@code null} if none matches
   */
  @Nullable
  static ReturnValueHandler select(Iterable<ReturnValueHandler> handlers,
          @Nullable Object handler, @Nullable Object returnValue) {
    if (returnValue != NONE_RETURN_VALUE) {
      if (handler != null) {
        // match handler and return-value
        for (ReturnValueHandler returnValueHandler : handlers) {
          if (returnValueHandler instanceof SmartReturnValueHandler smartHandler) {
            // smart handler
            if (smartHandler.supportsHandler(handler, returnValue)) {
              return returnValueHandler;
            }
          }
          else {
            if (returnValueHandler.supportsHandler(handler)
                    || returnValueHandler.supportsReturnValue(returnValue)) {
              return returnValueHandler;
            }
          }
        }
      }
      else {
        // match return-value only
        for (ReturnValueHandler returnValueHandler : handlers) {
          if (returnValueHandler.supportsReturnValue(returnValue)) {
            return returnValueHandler;
          }
        }
      }
    }
    else if (handler != null) {
      // match handler only
      for (ReturnValueHandler returnValueHandler : handlers) {
        if (returnValueHandler.supportsHandler(handler)) {
          return returnValueHandler;
        }
      }
    }
    return null;
  }

}
