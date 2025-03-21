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

package infra.web.handler.result;

import infra.lang.Nullable;
import infra.web.ReturnValueHandler;

/**
 * let user determine the handler and returnValue relationship
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ReturnValueHandler#supportsHandler(Object)
 * @see ReturnValueHandler#supportsReturnValue(Object)
 * @since 4.0 2022/4/24 22:26
 */
public interface SmartReturnValueHandler extends ReturnValueHandler {

  /**
   * handle handler and its return value
   * <p>
   * default is {@code supportsHandler} or {@code supportsReturnValue}
   * returns {@code true}
   * </p>
   *
   * @param handler target handler
   * @param returnValue handler's return value
   */
  default boolean supportsHandler(@Nullable Object handler, @Nullable Object returnValue) {
    return supportsHandler(handler) || supportsReturnValue(returnValue);
  }

  @Override
  default boolean supportsHandler(@Nullable Object handler) {
    return false;
  }

}
