/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.jdbc.support;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author TODAY 2021/6/2 21:34
 */
@FunctionalInterface
public interface ResultSetHandler {

  /**
   * Implementations must implement this method to process each row of data in the
   * ResultSet. This method should not call {@code next()} on the ResultSet; it is
   * only supposed to extract values of the current row.
   * <p>
   * Exactly what the implementation chooses to do is up to it: A trivial
   * implementation might simply count rows, while another implementation might
   * build an XML document.
   *
   * @param rs
   *         the ResultSet to process (pre-initialized for the current row)
   *
   * @throws SQLException
   *         if a SQLException is encountered getting column values (that is,
   *         there's no need to catch SQLException)
   */
  void handleResult(final ResultSet rs) throws SQLException;

}
