/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.result;

import infra.web.ReturnValueHandler;
import infra.web.handler.method.HandlerMethod;

/**
 * just for HandlerMethod return-value handling
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-12-13 13:52
 */
public interface HandlerMethodReturnValueHandler extends ReturnValueHandler {

  @Override
  default boolean supportsHandler(Object handler) {
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      return supportsHandlerMethod(handlerMethod);
    }
    return false;
  }

  /**
   * Whether the given {@linkplain HandlerMethod method} is supported by this handler.
   *
   * @return {@code true} if this handler supports the supplied return type;
   * {@code false} otherwise
   * @see HandlerMethod
   */
  default boolean supportsHandlerMethod(HandlerMethod handler) {
    return false;
  }

}
