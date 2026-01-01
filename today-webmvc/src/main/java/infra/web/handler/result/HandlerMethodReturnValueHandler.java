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
