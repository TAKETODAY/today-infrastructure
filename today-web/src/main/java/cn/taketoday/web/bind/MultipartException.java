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

package cn.taketoday.web.bind;

import java.io.Serial;

import cn.taketoday.http.converter.HttpMessageNotReadableException;

/**
 * Multipart cannot be parsed include
 * {@link cn.taketoday.web.multipart.MultipartFile} and normal part
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/1/17 10:41
 */
public class MultipartException extends HttpMessageNotReadableException {

  @Serial
  private static final long serialVersionUID = 1L;

  public MultipartException(String message) {
    super(message);
  }

  public MultipartException(String message, Throwable cause) {
    super(message, cause);
  }
}
