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

package cn.taketoday.beans;

import java.io.Serial;

/**
 * Property Cannot Readable
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/2 23:54
 */
public class PropertyCannotReadableException extends PropertyException {
  @Serial
  private static final long serialVersionUID = 1L;

  public PropertyCannotReadableException() { }

  public PropertyCannotReadableException(String message) {
    super(message);
  }

  public PropertyCannotReadableException(Throwable cause) {
    super(cause);
  }

  public PropertyCannotReadableException(String message, Throwable cause) {
    super(message, cause);
  }

}
