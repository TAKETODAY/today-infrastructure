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
package cn.taketoday.web;

import java.io.Serial;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * @author TODAY
 * @since 2019-07-20 15:51
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends ResponseStatusException {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String UNAUTHORIZED = HttpStatus.UNAUTHORIZED.getReasonPhrase();

  public UnauthorizedException() {
    super(HttpStatus.UNAUTHORIZED, UNAUTHORIZED);
  }

  public UnauthorizedException(String message) {
    super(HttpStatus.UNAUTHORIZED, message);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(HttpStatus.UNAUTHORIZED, message, cause);
  }

  public UnauthorizedException(Throwable cause) {
    super(HttpStatus.UNAUTHORIZED, UNAUTHORIZED, cause);
  }

}
