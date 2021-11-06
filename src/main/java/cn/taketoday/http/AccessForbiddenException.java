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
package cn.taketoday.http;

import cn.taketoday.web.WebNestedRuntimeException;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * @author TODAY 2018-11-26 20:06
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessForbiddenException extends WebNestedRuntimeException {
  private static final long serialVersionUID = 1L;
  public static final String ACCESS_FORBIDDEN = "Access Forbidden";

  public AccessForbiddenException(Throwable cause) {
    super(cause);
  }

  public AccessForbiddenException(String message, Throwable cause) {
    super(message, cause);
  }

  public AccessForbiddenException(String message) {
    super(message);
  }

  public AccessForbiddenException() {
    super(ACCESS_FORBIDDEN);
  }

}
