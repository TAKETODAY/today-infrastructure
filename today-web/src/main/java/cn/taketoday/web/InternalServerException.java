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
package cn.taketoday.web;

import java.io.Serial;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-12-02 09:14
 */
public class InternalServerException extends ResponseStatusException {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

  public InternalServerException(@Nullable String message, @Nullable Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
  }

  public InternalServerException(@Nullable String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  public InternalServerException() {
    super(HttpStatus.INTERNAL_SERVER_ERROR, null);
  }

  public static InternalServerException failed() {
    return new InternalServerException();
  }

  public static InternalServerException failed(@Nullable String msg) {
    return new InternalServerException(msg);
  }

  public static InternalServerException failed(@Nullable String msg, @Nullable Throwable cause) {
    return new InternalServerException(msg, cause);
  }

}
