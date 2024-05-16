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

package cn.taketoday.web.reactive.function.client;

import cn.taketoday.core.NestedRuntimeException;

/**
 * Abstract base class for exception published by {@link WebClient} in case of errors.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class WebClientException extends NestedRuntimeException {

  /**
   * Construct a new instance of {@code WebClientException} with the given message.
   *
   * @param msg the message
   */
  public WebClientException(String msg) {
    super(msg);
  }

  /**
   * Construct a new instance of {@code WebClientException} with the given message
   * and exception.
   *
   * @param msg the message
   * @param ex the exception
   */
  public WebClientException(String msg, Throwable ex) {
    super(msg, ex);
  }

}
