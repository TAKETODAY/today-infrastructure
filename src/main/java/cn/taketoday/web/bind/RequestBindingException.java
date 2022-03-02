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

package cn.taketoday.web.bind;

import java.io.Serial;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.web.ErrorResponse;

/**
 * Fatal binding exception, thrown when we want to
 * treat binding exceptions as unrecoverable.
 *
 * <p>Extends ServletException for convenient throwing in any Servlet resource
 * (such as a Filter), and NestedServletException for proper root cause handling
 * (as the plain ServletException doesn't expose its root cause at all).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 22:54
 */
public class RequestBindingException extends NestedRuntimeException implements ErrorResponse {
  @Serial
  private static final long serialVersionUID = 1L;
  private final ProblemDetail body = ProblemDetail.forRawStatusCode(getRawStatusCode());

  /**
   * Constructor for RequestBindingException.
   *
   * @param msg the detail message
   */
  public RequestBindingException(String msg) {
    super(msg);
  }

  /**
   * Constructor for RequestBindingException.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public RequestBindingException(String msg, Throwable cause) {
    super(msg, cause);
  }

  @Override
  public int getRawStatusCode() {
    return HttpStatus.BAD_REQUEST.value();
  }

  @Override
  public ProblemDetail getBody() {
    return this.body;
  }

}

