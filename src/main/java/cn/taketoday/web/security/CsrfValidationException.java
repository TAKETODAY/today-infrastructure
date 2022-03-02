/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.security;

import java.io.Serial;

import cn.taketoday.web.AccessForbiddenException;

/**
 * This exception is thrown by the CSRF token validation fails.
 * By default, this will result in a 403 status code sent to the
 * client. The application can provide a custom exception mapper
 * for this exception type to customize this default behavior.
 *
 * @author TODAY 2021/10/4 14:25
 * @since 4.0
 */
public class CsrfValidationException extends AccessForbiddenException {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Create a new CsrfValidationException
   *
   * @param message the detail message
   */
  public CsrfValidationException(String message) {
    super(message);
  }

}
