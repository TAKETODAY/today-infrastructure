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

package cn.taketoday.jdbc;

import cn.taketoday.lang.Nullable;

/**
 * jdbc parameter bind failed exception
 *
 * @author TODAY 2021/8/25 22:19
 * @since 4.0
 */
public class ParameterBindFailedException extends PersistenceException {

  public ParameterBindFailedException(@Nullable String message) {
    super(message);
  }

  public ParameterBindFailedException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

}
