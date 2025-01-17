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

package infra.web;

import infra.lang.Contract;
import infra.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/7 23:06
 */
public interface HandlerWrapper {

  Object getRawHandler();

  /**
   * unwrap handler
   *
   * @param handler maybe a wrapped handler
   * @return unwrapped handler
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
