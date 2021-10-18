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

package cn.taketoday.jdbc.support;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author TODAY 2021/6/2 21:34
 */
@FunctionalInterface
public interface ResultSetExtractor<T> {

  /**
   * Implementations must implement this method to process the entire ResultSet.
   *
   * @param rs ResultSet to extract data from. Implementations should not close
   * this: it will be closed by the calling JdbcTemplate.
   * @return an arbitrary result object, or {@code null} if none (the extractor
   * will typically be stateful in the latter case).
   * @throws SQLException if a SQLException is encountered getting column values or
   * navigating (that is, there's no need to catch SQLException)
   * @throws SQLException in case of custom exceptions
   */
  T extractData(final ResultSet rs) throws SQLException;

}
