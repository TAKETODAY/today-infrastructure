/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc.type;

import java.sql.ResultSet;
import java.sql.SQLException;

import infra.beans.BeanProperty;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/23 14:54
 */
public interface SmartTypeHandler<T> extends TypeHandler<T> {

  /**
   * Test this handler can handle input property
   *
   * @param property bean property
   */
  boolean supportsProperty(BeanProperty property);

  /**
   * apply result to a wrapped object
   *
   * @param value a wrapped object
   * @param rs database result set
   * @param columnIndex idx
   * @throws SQLException database read failed
   */
  default void applyResult(T value, ResultSet rs, int columnIndex) throws SQLException {
    throw new UnsupportedOperationException();
  }

}
