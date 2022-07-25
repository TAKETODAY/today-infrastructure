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

/**
 * @author TODAY
 * @since 2018-10-30 16:51
 */
public class BadRequestException extends ResponseStatusException {
  @Serial
  private static final long serialVersionUID = 1L;

  public static final String BAD_REQUEST = HttpStatus.BAD_REQUEST.getReasonPhrase();

  public BadRequestException() {
    super(HttpStatus.BAD_REQUEST, BAD_REQUEST, null);
  }

  public BadRequestException(String message) {
    super(HttpStatus.BAD_REQUEST, message, null);
  }

  public BadRequestException(Throwable cause) {
    super(HttpStatus.BAD_REQUEST, BAD_REQUEST, cause);
  }

  public BadRequestException(String message, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, message, cause);
  }

}
