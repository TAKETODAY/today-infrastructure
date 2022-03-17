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

package cn.taketoday.web.reactive.function.client;

import java.io.Serial;

import cn.taketoday.core.NestedRuntimeException;

/**
 * Abstract base class for exception published by {@link WebClient} in case of errors.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class WebClientException extends NestedRuntimeException {

  @Serial
  private static final long serialVersionUID = 472776714118912855L;

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
