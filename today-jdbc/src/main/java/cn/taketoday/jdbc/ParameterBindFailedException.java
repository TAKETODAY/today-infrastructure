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

package cn.taketoday.jdbc;

import java.io.Serial;

/**
 * jdbc parameter bind failed exception
 *
 * @author TODAY 2021/8/25 22:19
 * @since 4.0
 */
public class ParameterBindFailedException extends PersistenceException {

  @Serial
  private static final long serialVersionUID = 1L;

  public ParameterBindFailedException() { }

  public ParameterBindFailedException(String message) {
    super(message);
  }

  public ParameterBindFailedException(String message, Throwable cause) {
    super(message, cause);
  }

  public ParameterBindFailedException(Throwable cause) {
    super(cause);
  }

}
