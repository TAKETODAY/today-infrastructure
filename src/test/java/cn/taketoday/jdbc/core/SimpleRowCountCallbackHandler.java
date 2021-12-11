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

package cn.taketoday.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Simple row count callback handler for testing purposes.
 * Does not call any JDBC methods on the given ResultSet.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class SimpleRowCountCallbackHandler implements RowCallbackHandler {

  private int count;

  @Override
  public void processRow(ResultSet rs) throws SQLException {
    count++;
  }

  public int getCount() {
    return count;
  }

}
